package com.smartdata.smartruledatagen.controller;

import com.smartdata.smartruledatagen.config.GeneratorConfigLoader;
import com.smartdata.smartruledatagen.dto.ConsumePtsRequest;
import com.smartdata.smartruledatagen.dto.DataGenRequest;
import com.smartdata.smartruledatagen.dto.DataGenResponse;
import com.smartdata.smartruledatagen.generator.GenericDataGenerator;
import com.smartdata.smartruledatagen.model.CustomerData;
import com.smartdata.smartruledatagen.model.rules.FieldRule;
import com.smartdata.smartruledatagen.model.rules.GeneratorDefinition;
import com.smartdata.smartruledatagen.model.rules.ReferenceDataFieldRule;
import com.smartdata.smartruledatagen.service.ReferenceDataManager;
import com.smartdata.smartruledatagen.service.SqlTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/datagen")
@RequiredArgsConstructor
@Slf4j
public class DataGenController {

    private final GeneratorConfigLoader generatorConfigLoader;
    private final GenericDataGenerator genericDataGenerator;
    private final ReferenceDataManager referenceDataManager;
    private final SqlTemplateRepository sqlTemplateRepository;
    private final Map<String, JdbcTemplate> jdbcTemplates;

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @PostMapping("/consume-pts")
    public DataGenResponse generateConsumePtsData(@RequestBody ConsumePtsRequest request) {
        log.info("generateConsumePtsData request: {}", request);
        DataGenResponse response = new DataGenResponse();
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplates.get("ckJdbcTemplate");
            if (request.isExecuteInsert() && jdbcTemplate == null) {
                response.setSuccess(false);
                response.setMessage("未找到 ClickHouse 数据源配置 (ckJdbcTemplate)");
                return response;
            }

            String statsMonthStr = request.getStatistics_month();
            YearMonth yearMonth = YearMonth.parse(statsMonthStr);
            DateTimeFormatter fullTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            List<EmployeeRecord> employeeRecords = new ArrayList<>();
            Random random = new Random();

            // 1. 生成员工明细数据
            for (int i = 0; i < request.getCompanyNum(); i++) {
                long companyId = Math.abs(ThreadLocalRandom.current().nextLong(1000000000000L, 9999999999999L));
                String companyName = "企业名称" + companyId;

                for (int j = 0; j < request.getDeptNum(); j++) {
                    long deptId = Math.abs(ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L));
                    String deptName = "部门" + deptId;

                    for (int k = 0; k < request.getEmployeeNum(); k++) {
                        EmployeeRecord record = new EmployeeRecord();
                        record.customerId = request.getCustomer_id();
                        record.customerName = request.getCustomer_name();
                        record.companyId = companyId;
                        record.companyName = companyName;
                        record.deptId = deptId;
                        record.deptName = deptName;
                        record.employeeId = Math.abs(ThreadLocalRandom.current().nextLong(100000000000L, 999999999999L));
                        record.employeeName = "员工" + record.employeeId;
                        record.licenseType = "201";
                        record.licenseNumber = UUID.randomUUID().toString().substring(0, 16); // 模拟加密串
                        
                        record.totalCount = random.nextInt(10);
                        record.employeeDeclareCount = random.nextInt(5);
                        record.specialDeductionDeclareCount = random.nextInt(5);
                        record.taxCalculateCount = random.nextInt(5);
                        record.taxReportCount = random.nextInt(5);

                        // 随机生成月份内的日期和时间
                        int day = random.nextInt(yearMonth.lengthOfMonth()) + 1;
                        int hour = random.nextInt(24);
                        int min = random.nextInt(60);
                        int sec = random.nextInt(60);
                        LocalDateTime randomDateTime = yearMonth.atDay(day).atTime(hour, min, sec);
                        String timeStr = randomDateTime.format(fullTimeFormatter);
                        
                        record.totalLastUpdate = timeStr;
                        record.employeeDeclareLastUpdate = timeStr;
                        record.specialDeductionDeclareLastUpdate = timeStr;
                        record.taxCalculateLastUpdate = timeStr;
                        record.taxReportLastUpdate = timeStr;
                        
                        record.statisticsMonth = statsMonthStr;
                        record.eventMonth = statsMonthStr;
                        record.createTime = yearMonth.atDay(day).format(dayFormatter);

                        employeeRecords.add(record);
                    }
                }
            }

            List<String> allSqls = new ArrayList<>();

            // 2. 生成明细表 SQL
            String employeeSqlTemplate = "INSERT INTO `default`.ads_consume_pts_original_dept_employee_month_group_mi_final " +
                    "(customer_id, customer_name, company_id, company_name, dept_id, dept_name, employee_id, employee_name, license_type, license_number, total_count, total_last_update, employee_declare_count, employee_declare_last_update, special_deduction_declare_count, special_deduction_declare_last_update, tax_calculate_count, tax_calculate_last_update, tax_report_count, tax_report_last_update, statistics_month, create_time, event_month) " +
                    "VALUES (%d, '%s', %d, '%s', %d, '%s', %d, '%s', '%s', '%s', %d, '%s', %d, '%s', %d, '%s', %d, '%s', %d, '%s', '%s', '%s', '%s')";

            for (EmployeeRecord r : employeeRecords) {
                allSqls.add(String.format(employeeSqlTemplate,
                        r.customerId, r.customerName, r.companyId, r.companyName, r.deptId, r.deptName, r.employeeId, r.employeeName,
                        r.licenseType, r.licenseNumber, r.totalCount, r.totalLastUpdate, r.employeeDeclareCount, r.employeeDeclareLastUpdate,
                        r.specialDeductionDeclareCount, r.specialDeductionDeclareLastUpdate, r.taxCalculateCount, r.taxCalculateLastUpdate,
                        r.taxReportCount, r.taxReportLastUpdate, r.statisticsMonth, r.createTime, r.eventMonth));
            }

            // 3. 聚合生成部门汇总数据 SQL
            Map<String, AggregatedData> deptAggregates = new HashMap<>();
            for (EmployeeRecord r : employeeRecords) {
                String key = r.companyId + "_" + r.deptId;
                AggregatedData agg = deptAggregates.computeIfAbsent(key, k -> {
                    AggregatedData d = new AggregatedData();
                    d.customerId = r.customerId;
                    d.customerName = r.customerName;
                    d.companyId = r.companyId;
                    d.companyName = r.companyName;
                    d.deptId = r.deptId;
                    d.deptName = r.deptName;
                    d.statisticsMonth = r.statisticsMonth;
                    d.createTime = r.createTime;
                    d.eventMonth = r.eventMonth;
                    return d;
                });
                agg.totalCount += r.totalCount;
                agg.employeeDeclareCount += r.employeeDeclareCount;
                agg.specialDeductionDeclareCount += r.specialDeductionDeclareCount;
                agg.taxCalculateCount += r.taxCalculateCount;
                agg.taxReportCount += r.taxReportCount;
            }

            String deptSqlTemplate = "INSERT INTO `default`.ads_consume_pts_original_dept_month_group_mi_final " +
                    "(customer_id, customer_name, company_id, company_name, dept_id, dept_name, total_count, employee_declare_count, special_deduction_declare_count, tax_calculate_count, tax_report_count, statistics_month, create_time, event_month) " +
                    "VALUES (%d, '%s', %d, '%s', %d, '%s', %d, %d, %d, %d, %d, '%s', '%s', '%s')";

            for (AggregatedData r : deptAggregates.values()) {
                allSqls.add(String.format(deptSqlTemplate,
                        r.customerId, r.customerName, r.companyId, r.companyName, r.deptId, r.deptName,
                        r.totalCount, r.employeeDeclareCount, r.specialDeductionDeclareCount, r.taxCalculateCount, r.taxReportCount,
                        r.statisticsMonth, r.createTime, r.eventMonth));
            }

            // 4. 聚合生成企业汇总数据 SQL
            Map<Long, AggregatedData> companyAggregates = new HashMap<>();
            for (EmployeeRecord r : employeeRecords) {
                AggregatedData agg = companyAggregates.computeIfAbsent(r.companyId, k -> {
                    AggregatedData d = new AggregatedData();
                    d.customerId = r.customerId;
                    d.customerName = r.customerName;
                    d.companyId = r.companyId;
                    d.companyName = r.companyName;
                    d.statisticsMonth = r.statisticsMonth;
                    d.createTime = r.createTime;
                    d.eventMonth = r.eventMonth;
                    return d;
                });
                agg.totalCount += r.totalCount;
                agg.employeeDeclareCount += r.employeeDeclareCount;
                agg.specialDeductionDeclareCount += r.specialDeductionDeclareCount;
                agg.taxCalculateCount += r.taxCalculateCount;
                agg.taxReportCount += r.taxReportCount;
            }

            String companySqlTemplate = "INSERT INTO `default`.ads_consume_pts_original_company_month_group_mi_final " +
                    "(customer_id, customer_name, company_id, company_name, total_count, employee_declare_count, special_deduction_declare_count, tax_calculate_count, tax_report_count, statistics_month, create_time, event_month) " +
                    "VALUES (%d, '%s', %d, '%s', %d, %d, %d, %d, %d, '%s', '%s', '%s')";

            for (AggregatedData r : companyAggregates.values()) {
                allSqls.add(String.format(companySqlTemplate,
                        r.customerId, r.customerName, r.companyId, r.companyName,
                        r.totalCount, r.employeeDeclareCount, r.specialDeductionDeclareCount, r.taxCalculateCount, r.taxReportCount,
                        r.statisticsMonth, r.createTime, r.eventMonth));
            }

            // 5. 执行入库
            int successCount = 0;
            if (request.isExecuteInsert()) {
                for (String sql : allSqls) {
                    try {
                        jdbcTemplate.execute(sql);
                        successCount++;
                    } catch (Exception e) {
                        log.error("ClickHouse SQL execution failed: {}", sql, e);
                    }
                }
            }

            response.setSuccess(true);
            response.setMessage("ClickHouse 数据生成成功");
            response.setGeneratedCount(allSqls.size());
            response.setSuccessInsertCount(successCount);
            response.setSqls(allSqls);

        } catch (Exception e) {
            log.error("ClickHouse data generation failed", e);
            response.setSuccess(false);
            response.setMessage("生成失败: " + e.getMessage());
        }
        return response;
    }

    private static class EmployeeRecord {
        long customerId;
        String customerName;
        long companyId;
        String companyName;
        long deptId;
        String deptName;
        long employeeId;
        String employeeName;
        String licenseType;
        String licenseNumber;
        int totalCount;
        String totalLastUpdate;
        int employeeDeclareCount;
        String employeeDeclareLastUpdate;
        int specialDeductionDeclareCount;
        String specialDeductionDeclareLastUpdate;
        int taxCalculateCount;
        String taxCalculateLastUpdate;
        int taxReportCount;
        String taxReportLastUpdate;
        String statisticsMonth;
        String createTime;
        String eventMonth;
    }

    private static class AggregatedData {
        long customerId;
        String customerName;
        long companyId;
        String companyName;
        long deptId;
        String deptName;
        long totalCount = 0;
        long employeeDeclareCount = 0;
        long specialDeductionDeclareCount = 0;
        long taxCalculateCount = 0;
        long taxReportCount = 0;
        String statisticsMonth;
        String createTime;
        String eventMonth;
    }

    @PostMapping("/generate")
    public DataGenResponse generateData(@RequestBody DataGenRequest request) {
        log.info("generateData request: {}", request);
        DataGenResponse response = new DataGenResponse();
        try {
            // 1. 验证生成器是否存在
            GeneratorDefinition definition = generatorConfigLoader.getRuleConfig().getGenerators().get(request.getGeneratorName());
            if (definition == null) {
                response.setSuccess(false);
                response.setMessage("生成器不存在: " + request.getGeneratorName());
                return response;
            }

            // 获取对应的 JdbcTemplate
            String dbKey = definition.getDbKey(); // e.g., "db1"
            JdbcTemplate jdbcTemplate = jdbcTemplates.get(dbKey + "JdbcTemplate");
            if (request.isExecuteInsert() && jdbcTemplate == null) {
                response.setSuccess(false);
                response.setMessage("未找到对应的数据源配置: " + dbKey);
                return response;
            }

            // 1.5 针对汇总类生成器的特殊处理
            if ("customerReviewExpireSummary".equals(request.getGeneratorName())) {
                return handleSummaryGeneration(request, definition, jdbcTemplate);
            }

            // 2. 获取客户数据 (支持通过 extraParams 指定经理或具体客户)
            List<CustomerData> candidates = new ArrayList<>();
            Map<String, Object> extraParams = request.getExtraParams() != null ? request.getExtraParams() : new HashMap<>();

            String customerManageId = (String) extraParams.get("customerManageId");

              if (customerManageId != null && !customerManageId.trim().isEmpty() && !"null".equalsIgnoreCase(customerManageId)) {
                  // 情况 A: 仅提供了经理 ID
                  candidates = referenceDataManager.getCustomersByManageId(customerManageId.trim());
                  log.info("Found {} customers for manager {}", candidates.size(), customerManageId);
              } else {
                 // 情况 B: 兜底使用 regionCode 逻辑
                if (request.getRegionCode() == null || request.getRegionCode().isEmpty()) {
                    response.setSuccess(false);
                    response.setMessage("地区代码不能为空 (当未指定 customerManageId 时)");
                    return response;
                }
                if (!referenceDataManager.hasRegionData(request.getRegionCode())) {
                    response.setSuccess(false);
                    response.setMessage("地区代码不存在或该地区无客户经理数据: " + request.getRegionCode());
                    return response;
                }
                candidates = referenceDataManager.getCustomersByRegion(request.getRegionCode());
                log.info("Total candidates for region {}: {}", request.getRegionCode(), candidates.size());
            }

            // 针对业务逻辑的进一步筛选
            if ("tradeKpiPayment".equals(request.getGeneratorName())) {
                candidates = candidates.stream()
                        .filter(c -> "1".equals(c.getCustomerType()))
                        .collect(Collectors.toList());
                log.info("Candidates after filtering type=1: {}", candidates.size());
            }
            // 针对 csBoardAuthMonthly 生成器，只筛选 type=2/3 的客户
            if ("csBoardAuthMonthly".equals(request.getGeneratorName())) {
                candidates = candidates.stream()
                        .filter(c -> "2".equals(c.getCustomerType()) || "3".equals(c.getCustomerType()))
                        .collect(Collectors.toList());
            }

            if (candidates.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("该地区下未找到任何客户数据" + ("tradeKpiPayment".equals(request.getGeneratorName()) ? " (Type=1)" : ""));
                return response;
            }

            // 3. 确定生成数量和提示
            int requestCount = request.getCount();
            int actualCount = requestCount;
            String message = "生成成功";
            
            if (requestCount > candidates.size()) {
                actualCount = candidates.size();
                message = String.format("请求生成 %d 条，但该地区只有 %d 条可用客户数据。已生成 %d 条。", requestCount, candidates.size(), actualCount);
            }

            // 4. 准备预置数据 (乱序并截取)
            List<CustomerData> selectedCustomers = new ArrayList<>(candidates);
            Collections.shuffle(selectedCustomers);
            selectedCustomers = selectedCustomers.subList(0, actualCount);

            // 5. 映射字段名
            String idFieldName = "customer_id"; // 默认
            String nameFieldName = "customer_name"; // 默认

            // 尝试从生成器定义中智能查找字段名
            for (FieldRule rule : definition.getFields()) {
                if (rule instanceof ReferenceDataFieldRule refRule) {
                    if ("CustomerData".equalsIgnoreCase(refRule.getRefDataType())) {
                        if ("id".equalsIgnoreCase(refRule.getFieldToPopulate())) {
                            idFieldName = refRule.getName();
                        } else if ("name".equalsIgnoreCase(refRule.getFieldToPopulate())) {
                            nameFieldName = refRule.getName();
                        }
                    }
                }
            }
            
            // 特殊处理 csOrderPayment，因为它可能有多个 CustomerData 引用
            if ("csOrderPayment".equals(request.getGeneratorName())) {
                idFieldName = "main_customer_id";
                nameFieldName = "main_customer_name";
            }

            List<Map<String, Object>> preDefinedRecords = new ArrayList<>();
            for (CustomerData customer : selectedCustomers) {
                Map<String, Object> record = new HashMap<>();
                record.put(idFieldName, customer.getCustomerId());
                record.put(nameFieldName, customer.getCustomerName());
                
                // 将客户经理ID预置进去
                String finalManageId = (customerManageId != null && !customerManageId.trim().isEmpty() && !"null".equalsIgnoreCase(customerManageId))
                        ? customerManageId.trim()
                        : customer.getCustomerManageId();

                if (finalManageId != null) {
                    record.put("customer_manage_id", finalManageId);
                    // 同时也预置到生成器常用的经理 ID 字段中，这样会跳过 YAML 中的随机/引用规则
                    record.put("cust_mgr_id", finalManageId);
                    record.put("signer_id", finalManageId);
                    record.put("income_khjl_id", finalManageId);
                    record.put("income_khjl_id_detail", finalManageId);
                }
                if (customer.getServyouNum() != null) {
                    record.put("servyou_num", customer.getServyouNum());
                }
                if (customer.getSaleAreaId() != null) {
                    record.put("sale_area_id", customer.getSaleAreaId());
                }
                if (customer.getProvinceCityAreaCode() != null) {
                    record.put("province_city_area_code", customer.getProvinceCityAreaCode());
                }

                    preDefinedRecords.add(record);
            }

            // 6. 生成 SQL
            Map<String, Object> params = new HashMap<>();
            // 传递 regionCode 供规则使用 (e.g. lookupProvinceByRegion)
            params.put("regionCode", request.getRegionCode());
            params.put("bigRegionCode", request.getRegionCode()); // 兼容旧名称

            // 合并扩展参数
            if (request.getExtraParams() != null) {
                params.putAll(request.getExtraParams());
            }

            List<String> sqls = genericDataGenerator.generateSqls(definition, actualCount, params, preDefinedRecords);

            // 7. 执行插入 (如果需要)
            int successInsertCount = 0;
            if (request.isExecuteInsert()) {
                for (String sql : sqls) {
                    try {
                        // 移除可能的末尾分号
                        String cleanSql = sql.trim();
                        if (cleanSql.endsWith(";")) {
                            cleanSql = cleanSql.substring(0, cleanSql.length() - 1);
                        }
                        jdbcTemplate.execute(cleanSql);
                        successInsertCount++;
                    } catch (Exception e) {
                        log.error("Failed to execute SQL: {}", sql, e);
                        // 可以选择继续或中断，这里选择继续并统计成功数
                    }
                }
            }

            response.setSuccess(true);
            response.setMessage(message);
            // generatedCount 应该返回生成的“记录数”（主表数据量），即 actualCount
            // 而不是 sqls.size() (因为现在可能包含详情表等多条SQL)
            response.setGeneratedCount(actualCount);
            response.setSuccessInsertCount(successInsertCount);
            response.setSqls(sqls);

        } catch (Exception e) {
            log.error("Generation failed", e);
            response.setSuccess(false);
            response.setMessage("生成失败: " + e.getMessage());
        }
        return response;
    }

    private DataGenResponse handleSummaryGeneration(DataGenRequest request, GeneratorDefinition definition, JdbcTemplate jdbcTemplate) {
        DataGenResponse response = new DataGenResponse();
        Map<String, Object> extraParams = request.getExtraParams();

        if (extraParams == null || !extraParams.containsKey("endDateMonth") || extraParams.get("endDateMonth") == null) {
            response.setSuccess(false);
            response.setMessage("参数 endDateMonth 不能为空");
            return response;
        }

        String endDateMonth = String.valueOf(extraParams.get("endDateMonth")).trim();
        if (endDateMonth.isEmpty() || "null".equalsIgnoreCase(endDateMonth)) {
            response.setSuccess(false);
            response.setMessage("参数 endDateMonth 不能为空或无效");
            return response;
        }
        // 优先从 extraParams 获取 regionCode，如果没有则取外层的
        String bigRegionCode = extraParams.containsKey("regionCode")
                ? String.valueOf(extraParams.get("regionCode"))
                : request.getRegionCode();

        if (bigRegionCode == null || bigRegionCode.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("参数 regionCode 不能为空");
            return response;
        }

        try {
            // 1. 执行删除操作 (如果配置了)
            if (definition.getExtraSqlTemplateKeys() != null && !definition.getExtraSqlTemplateKeys().isEmpty()) {
                for (String deleteKey : definition.getExtraSqlTemplateKeys()) {
                    String deleteSqlTemplate = sqlTemplateRepository.getTemplate(deleteKey);
                    if (deleteSqlTemplate != null) {
                        String deleteSql = deleteSqlTemplate
                                .replace("{end_date_month}", endDateMonth)
                                .replace("{big_region_code}", bigRegionCode);
                        log.info("Executing summary delete SQL: {}", deleteSql);
                        jdbcTemplate.execute(deleteSql);
                    }
                }
            }

            // 2. 执行汇总插入操作
            String insertSqlTemplate = sqlTemplateRepository.getTemplate(definition.getSqlTemplateKey());
            if (insertSqlTemplate == null) {
                throw new RuntimeException("汇总插入模板未找到: " + definition.getSqlTemplateKey());
            }

            String insertSql = insertSqlTemplate
                    .replace("{end_date_month}", endDateMonth)
                    .replace("{big_region_code}", bigRegionCode);

            log.info("Executing summary insert SQL: {}", insertSql);
            // 使用 update 以获取受影响的行数
            int rows = jdbcTemplate.update(insertSql);

            response.setSuccess(true);
            response.setMessage("汇总数据生成成功");
            response.setGeneratedCount(rows);
            response.setSuccessInsertCount(rows);
            response.setSqls(Collections.singletonList(insertSql));

        } catch (Exception e) {
            log.error("Summary generation failed", e);
            response.setSuccess(false);
            response.setMessage("汇总生成失败: " + e.getMessage());
        }
        return response;
    }
}
