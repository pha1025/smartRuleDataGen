package com.smartdata.smartruledatagen.controller;

import com.smartdata.smartruledatagen.config.GeneratorConfigLoader;
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

import java.util.*;
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

             if (customerManageId != null) {
                 // 情况 A: 仅提供了经理 ID
                 candidates = referenceDataManager.getCustomersByManageId(customerManageId);
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
                
                // 将客户经理ID预置进去，键名为 "customer_manage_id"，以便规则中引用
                // 这样在规则中可以通过 expression: "customer_manage_id" 引用
                if (customer.getCustomerManageId() != null) {
                    record.put("customer_manage_id", customer.getCustomerManageId());
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

        if (extraParams == null || !extraParams.containsKey("endDateMonth")) {
            response.setSuccess(false);
            response.setMessage("参数 endDateMonth 不能为空");
            return response;
        }

        String endDateMonth = String.valueOf(extraParams.get("endDateMonth"));
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
