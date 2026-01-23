package com.smartdata.smartruledatagen.service;

import com.smartdata.smartruledatagen.model.CustMgrData;
import com.smartdata.smartruledatagen.model.CustomerData;
import com.smartdata.smartruledatagen.model.EnumMapping;
import com.smartdata.smartruledatagen.model.RegionProvinceData;

import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import java.util.Set;

@Service
@Slf4j
public class ReferenceDataManager {

    private final ExcelDataLoaderService excelDataLoaderService;

    @Value("${excel.cust-mgr-data-path}")
    private String custMgrDataPath;

    @Value("${excel.enum-data-path}")
    private String enumDataPath;

    @Value("${excel.customer-data-path:data/customer_data.xlsx}")
    private String customerDataPath;

    @Value("${excel.region-province-data-path:data/region_province.xlsx}")
    private String regionProvinceDataPath;

    // 存储客户经理层级数据，按大区代码分组
    private Map<String, List<CustMgrData>> custMgrDataByBigRegion;
    // 存储客户经理数据，按客户经理ID快速查找
    private Map<String, CustMgrData> custMgrDataById;
    // 存储枚举映射，按类别分组
    private Map<String, List<EnumMapping>> enumMappings;

    // 存储客户数据
    private Map<String, List<CustomerData>> customerDataByType;
    private Map<String, CustomerData> customerDataById;
    private Map<String, List<CustomerData>> customerDataByManageId; // 新增：按 ManageID 分组

    // 存储省份数据
    private Map<String, String> provinceCodeByBigRegion;

    // TODO: 如果 group_id 有独立的Excel，需要在这里加载
    // private Map<String, String> groupIdLookup; // cust_mgr_id -> group_id

    public ReferenceDataManager(ExcelDataLoaderService excelDataLoaderService) {
        this.excelDataLoaderService = excelDataLoaderService;
    }

    @PostConstruct
    public void init() {
        log.info("Loading reference data: Customer Manager Hierarchy from {}", custMgrDataPath);
        List<CustMgrData> loadedCustMgrs = excelDataLoaderService.loadCustMgrData(custMgrDataPath);
        this.custMgrDataByBigRegion = loadedCustMgrs.stream()
                .collect(Collectors.groupingBy(CustMgrData::getCustMgrBigRegionCode));
        this.custMgrDataById = loadedCustMgrs.stream()
                .collect(Collectors.toMap(CustMgrData::getCustMgrId, data -> data, (oldValue, newValue) -> newValue, ConcurrentHashMap::new));
        log.info("Loaded {} customer manager records.", loadedCustMgrs.size());


        log.info("Loading reference data: Enum Dictionaries from {}", enumDataPath);
        this.enumMappings = excelDataLoaderService.loadEnumMappings(enumDataPath);
        log.info("Loaded {} enum categories.", enumMappings.size());

        log.info("Loading reference data: Customer Data from {}", customerDataPath);
        List<CustomerData> loadedCustomers = excelDataLoaderService.loadCustomerData(customerDataPath);
        this.customerDataByType = loadedCustomers.stream()
                .filter(c -> c.getCustomerType() != null)
                .collect(Collectors.groupingBy(CustomerData::getCustomerType));
        this.customerDataById = loadedCustomers.stream()
                .filter(c -> c.getCustomerId() != null)
                .collect(Collectors.toMap(CustomerData::getCustomerId, data -> data, (oldValue, newValue) -> newValue, ConcurrentHashMap::new));
        this.customerDataByManageId = loadedCustomers.stream()
                .filter(c -> c.getCustomerManageId() != null)
                .collect(Collectors.groupingBy(CustomerData::getCustomerManageId));
        log.info("Loaded {} customer records.", loadedCustomers.size());

        log.info("Loading reference data: Region Province Data from {}", regionProvinceDataPath);
        List<RegionProvinceData> loadedRegionProvinces = excelDataLoaderService.loadRegionProvinceData(regionProvinceDataPath);
        if (loadedRegionProvinces.isEmpty()) {
            log.warn("Region Province Data file not found or empty. Using default mappings for testing.");
            this.provinceCodeByBigRegion = new HashMap<>();
            this.provinceCodeByBigRegion.put("004011006", "130000"); // 河北
            this.provinceCodeByBigRegion.put("004012020", "440000"); // 广东
            this.provinceCodeByBigRegion.put("004012022", "440000"); // 粤西?
        } else {
            this.provinceCodeByBigRegion = loadedRegionProvinces.stream()
                    .collect(Collectors.toMap(RegionProvinceData::getBigRegionCode, RegionProvinceData::getProvinceCityAreaCode, (v1, v2) -> v1));
        }
        log.info("Loaded {} region province records.", this.provinceCodeByBigRegion.size());
    }

    public List<CustMgrData> getCustMgrsByBigRegion(String bigRegionCode) {
        return custMgrDataByBigRegion.getOrDefault(bigRegionCode, new ArrayList<>());
    }

    public List<CustMgrData> getAllCustMgrs() {
        return custMgrDataById.values().stream().toList();
    }

    public Optional<CustMgrData> getCustMgrById(String custMgrId) {
        return Optional.ofNullable(custMgrDataById.get(custMgrId));
    }

    public List<EnumMapping> getEnumMappings(String category) {
        return enumMappings.getOrDefault(category, new ArrayList<>());
    }

    public Optional<EnumMapping> getEnumByCode(String category, String code) {
        return getEnumMappings(category).stream()
                .filter(e -> e.getCode().equals(code))
                .findFirst();
    }

    public Optional<EnumMapping> getEnumByName(String category, String name) {
        return getEnumMappings(category).stream()
                .filter(e -> e.getName().equals(name))
                .findFirst();
    }

    public List<CustomerData> getCustomersByType(String type) {
        return customerDataByType.getOrDefault(type, new ArrayList<>());
    }

    public Optional<CustomerData> getCustomerById(String customerId) {
        return Optional.ofNullable(customerDataById.get(customerId));
    }

    public List<CustomerData> getCustomersByRegion(String regionCode) {
        if (!custMgrDataByBigRegion.containsKey(regionCode)) {
            return new ArrayList<>();
        }
        List<CustMgrData> mgrs = custMgrDataByBigRegion.get(regionCode);
        Set<String> mgrIds = mgrs.stream().map(CustMgrData::getCustMgrId).collect(Collectors.toSet());

        List<CustomerData> result = new ArrayList<>();
        for (String mgrId : mgrIds) {
            if (customerDataByManageId.containsKey(mgrId)) {
                result.addAll(customerDataByManageId.get(mgrId));
            }
        }
        return result;
    }

    public String getProvinceCodeByBigRegion(String bigRegionCode) {
        return provinceCodeByBigRegion.getOrDefault(bigRegionCode, "440000"); // 默认广东
    }

    // TODO: getGroupIdByCustMgrId(String custMgrId)
}
