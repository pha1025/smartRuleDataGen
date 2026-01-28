package com.smartdata.smartruledatagen.service;

import com.smartdata.smartruledatagen.model.CustMgrData;
import com.smartdata.smartruledatagen.model.CustomerData;
import com.smartdata.smartruledatagen.model.EnumMapping;
import com.smartdata.smartruledatagen.model.RegionProvinceData;
import org.apache.poi.ss.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelDataLoaderService {

    private String cleanPath(String path) {
        if (path.startsWith("src/main/resources/")) {
            return path.substring("src/main/resources/".length());
        }
        return path;
    }

    public List<CustMgrData> loadCustMgrData(String filePath) {
        List<CustMgrData> dataList = new ArrayList<>();
        filePath = cleanPath(filePath);
        ClassPathResource resource = new ClassPathResource(filePath);
        
        try (InputStream fis = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(fis)) {

            // 遍历所有 Sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName(); // Sheet名为 Big Region Code
                
                boolean firstRow = true;
                for (Row row : sheet) {
                    if (firstRow) { // 跳过表头
                        firstRow = false;
                        continue;
                    }
                    if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                        continue; // 跳过空行
                    }

                    CustMgrData data = new CustMgrData();
                    // 根据变更：Sheet名称即为 Big Region Code
                    // 如果Excel第一列仍是大区代码，可以覆盖或校验；这里我们优先使用Sheet名作为大区代码，或者如果单元格为空则使用Sheet名
                    String regionCodeInCell = getCellValue(row.getCell(0));
                    if (regionCodeInCell != null && !regionCodeInCell.isEmpty()) {
                        data.setCustMgrBigRegionCode(regionCodeInCell);
                    } else {
                        data.setCustMgrBigRegionCode(sheetName);
                    }
                    
                    data.setCustMgrBigRegionName(getCellValue(row.getCell(1))); // 假设大区名称在第2列
                    data.setCustMgrOutletCode(getCellValue(row.getCell(2)));
                    data.setCustMgrOutletName(getCellValue(row.getCell(3)));
                    data.setCustMgrId(getCellValue(row.getCell(4)));
                    data.setCustMgrName(getCellValue(row.getCell(5)));

                    // 如果Excel中包含groupId/groupName，继续解析
                    data.setCustMgrGroupId(getCellValue(row.getCell(6)));
                    data.setCustMgrGroupName(getCellValue(row.getCell(7)));

                    dataList.add(data);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            // 抛出自定义异常或处理错误
        }
        return dataList;
    }

    public Map<String, List<EnumMapping>> loadEnumMappings(String filePath) {
        Map<String, List<EnumMapping>> enumMap = new HashMap<>();
        filePath = cleanPath(filePath);
        ClassPathResource resource = new ClassPathResource(filePath);

        try (InputStream fis = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(fis)) {

            // 假设每个sheet代表一个枚举类别 (e.g., sheet1: business_class, sheet2: order_type)
            // 或者在一个sheet中，第一列是 category, 第二列是 code, 第三列是 name
            // 这里我们以每个Sheet为一个Category为例
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String category = sheet.getSheetName(); // sheet名作为 category
                List<EnumMapping> mappings = new ArrayList<>();
                boolean firstRow = true;
                for (Row row : sheet) {
                    if (firstRow) {
                        firstRow = false;
                        continue;
                    }
                    if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                        continue;
                    }
                    EnumMapping mapping = new EnumMapping();
                    mapping.setCategory(category);
                    mapping.setCode(getCellValue(row.getCell(0))); // 假设第一列是 code
                    mapping.setName(getCellValue(row.getCell(1))); // 假设第二列是 name
                    mappings.add(mapping);
                }
                enumMap.put(category, mappings);
            }

        } catch (IOException e) {
            e.printStackTrace();
            // 抛出自定义异常或处理错误
        }
        return enumMap;
    }

    public List<CustomerData> loadCustomerData(String filePath) {
        List<CustomerData> dataList = new ArrayList<>();
        filePath = cleanPath(filePath);
        ClassPathResource resource = new ClassPathResource(filePath);

        try (InputStream fis = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(fis)) {

            // 遍历所有 Sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName(); // Sheet名为 Customer Type

                boolean firstRow = true;
                for (Row row : sheet) {
                    if (firstRow) {
                        firstRow = false;
                        continue;
                    }
                    if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                        continue;
                    }

                    CustomerData data = new CustomerData();
                    // 0: customer_id, 1: customer_name, 2: customer_type, 3: servyou_num, 4: sale_area_id, 5: customer_manage_id, 6: province_city_area_code
                    data.setCustomerId(getCellValue(row.getCell(0)));
                    data.setCustomerName(getCellValue(row.getCell(1)));
                    data.setProvinceCityAreaCode(getCellValue(row.getCell(2)));
                    
                    // 优先使用 Sheet 名作为 Type
                    String typeInCell = getCellValue(row.getCell(3));
                    if (typeInCell != null && !typeInCell.isEmpty()) {
                        data.setCustomerType(typeInCell);
                    } else {
                        data.setCustomerType(sheetName);
                    }
                    data.setServyouNum(getCellValue(row.getCell(4)));
                    data.setSaleAreaId(getCellValue(row.getCell(5)));
                    data.setCustomerManageId(getCellValue(row.getCell(6)));

                    dataList.add(data);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataList;
    }

    public List<RegionProvinceData> loadRegionProvinceData(String filePath) {
        List<RegionProvinceData> dataList = new ArrayList<>();
        // 如果文件不存在，返回空列表
        filePath = cleanPath(filePath);
        ClassPathResource resource = new ClassPathResource(filePath);
        
        if (!resource.exists()) {
            return dataList;
        }

        try (InputStream fis = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false;
                    continue;
                }
                if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                    continue;
                }

                RegionProvinceData data = new RegionProvinceData();
                // Assumed columns: 0: big_region_code, 1: province_city_area_code
                data.setBigRegionCode(getCellValue(row.getCell(0)));
                data.setProvinceCityAreaCode(getCellValue(row.getCell(1)));

                dataList.add(data);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataList;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        String value = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue()); // 防止数字被读成浮点数
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula(); // 可以考虑 evaluateFormulaCell
            case BLANK -> null;
            default -> null;
        };
        return value != null ? value.trim() : null;
    }
}
