package com.smartdata.smartruledatagen.generator;

import com.smartdata.smartruledatagen.generator.exception.DependencyNotMetException;
import com.smartdata.smartruledatagen.generator.util.ExpressionEvaluator;
import com.smartdata.smartruledatagen.generator.util.ValueFormatter;
import com.smartdata.smartruledatagen.model.CustMgrData;
import com.smartdata.smartruledatagen.model.CustomerData;
import com.smartdata.smartruledatagen.model.EnumMapping;
import com.smartdata.smartruledatagen.model.rules.*;
import com.smartdata.smartruledatagen.service.ReferenceDataManager;
import com.smartdata.smartruledatagen.service.SqlTemplateRepository;
import com.smartdata.smartruledatagen.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GenericDataGenerator {

    private final ReferenceDataManager referenceDataManager;
    private final SqlTemplateRepository sqlTemplateRepository;
    private final ExpressionEvaluator expressionEvaluator;
    private final Random random = new Random();

    public GenericDataGenerator(ReferenceDataManager referenceDataManager, SqlTemplateRepository sqlTemplateRepository) {
        this.referenceDataManager = referenceDataManager;
        this.sqlTemplateRepository = sqlTemplateRepository;
        this.expressionEvaluator = new ExpressionEvaluator(referenceDataManager); // 注入 ReferenceDataManager
    }

    public List<String> generateSqls(GeneratorDefinition generatorDefinition, int count, Map<String, Object> params) {
        return generateSqls(generatorDefinition, count, params, null);
    }

    public List<String> generateSqls(GeneratorDefinition generatorDefinition, int count, Map<String, Object> params, List<Map<String, Object>> preDefinedRecords) {
        List<String> sqls = new ArrayList<>();
        String template = sqlTemplateRepository.getTemplate(generatorDefinition.getSqlTemplateKey());
        if (template == null) {
            log.error("SQL template with key '{}' not found.", generatorDefinition.getSqlTemplateKey());
            throw new IllegalArgumentException("SQL template not found: " + generatorDefinition.getSqlTemplateKey());
        }

        List<FieldRule> fieldRules = generatorDefinition.getFields();
        Iterator<Map<String, Object>> preDefinedIterator = (preDefinedRecords != null) ? preDefinedRecords.iterator() : null;

        for (int i = 0; i < count; i++) {
            Map<String, Object> currentRecordData = new HashMap<>();
            if (preDefinedIterator != null && preDefinedIterator.hasNext()) {
                currentRecordData.putAll(preDefinedIterator.next());
            }

            Map<String, String> formattedValues = new HashMap<>(); // 存储格式化后的值，用于SQL String.format

            // 为了处理字段依赖，可能需要多轮处理或构建依赖图。
            // 简单起见，这里先尝试单次遍历，对于未满足依赖的字段，可以考虑后续的迭代优化。
            // 更健壮的实现会通过拓扑排序处理依赖关系。
            // 这里我们采用简单多次尝试，直到所有字段都被填充或者达到最大尝试次数。
            int maxAttempts = fieldRules.size() * 2; // 允许的尝试次数，避免死循环
            Set<String> populatedFields = new HashSet<>();

            for (int attempt = 0; attempt < maxAttempts && populatedFields.size() < fieldRules.size(); attempt++) {
                for (FieldRule rule : fieldRules) {
                    if (populatedFields.contains(rule.getName())) {
                        continue; // 已经生成过
                    }

                    // 优先使用预定义的值
                    if (currentRecordData.containsKey(rule.getName()) && currentRecordData.get(rule.getName()) != null) {
                        Object value = currentRecordData.get(rule.getName());
                        // 特殊处理：如果预置的是 _temp_cust_type 这种中间变量，它不需要 includeInSql，但需要放入 formattedValues (虽然可能不用)
                        // 主要是如果是 includeInSql=true 的字段，必须放入 formattedValues
                        if (rule.isIncludeInSql()) {
                            formattedValues.put(rule.getName(), ValueFormatter.formatForSql(value, rule.getSqlType(), rule.isNullable()));
                        }
                        populatedFields.add(rule.getName());
                        continue;
                    }

                    try {
                        Object value = generateFieldValue(rule, currentRecordData, params);
                        currentRecordData.put(rule.getName(), value);
                        if (rule.isIncludeInSql()) {
                            formattedValues.put(rule.getName(), ValueFormatter.formatForSql(value, rule.getSqlType(), rule.isNullable()));
                        }
                        populatedFields.add(rule.getName());
                    } catch (DependencyNotMetException e) {
                        // 依赖未满足，跳过当前字段，在下一轮尝试
                        log.debug("Dependency not met for field {}. Retrying later.", rule.getName());
                    } catch (Exception e) {
                        log.error("Error generating value for field {}: {}", rule.getName(), e.getMessage());
                        // 对于无法生成的值，暂时用 NULL 或默认空值填充，以避免中断整个生成
                        currentRecordData.put(rule.getName(), null);
                        if (rule.isIncludeInSql()) {
                            formattedValues.put(rule.getName(), ValueFormatter.formatForSql(null, rule.getSqlType(), rule.isNullable()));
                        }
                        populatedFields.add(rule.getName()); // 标记为已处理，防止无限重试
                    }
                }
            }

            if (populatedFields.size() < fieldRules.size()) {
                log.warn("Not all fields could be populated after {} attempts for record {}. Missing fields: {}",
                        maxAttempts, i, fieldRules.stream().filter(r -> !populatedFields.contains(r.getName())).map(FieldRule::getName).collect(Collectors.joining(", ")));
                // 确保所有缺失字段有默认的 NULL 或空字符串，以便SQL可以格式化
                for (FieldRule rule : fieldRules) {
                    if (!populatedFields.contains(rule.getName())) {
                        currentRecordData.put(rule.getName(), null);
                        if (rule.isIncludeInSql()) {
                            formattedValues.put(rule.getName(), ValueFormatter.formatForSql(null, rule.getSqlType(), rule.isNullable()));
                        }
                    }
                }
            }


            // 按照SQL模板的占位符顺序准备参数
            // 支持两种模式：
            // 1. 命名占位符 {fieldName} - 推荐，更灵活，支持 extraSqlTemplates
            // 2. 顺序占位符 %s - 旧模式，依赖 field definition order 和 includeInSql
            
            if (template.contains("{") && template.contains("}")) {
                // 模式 1: 命名替换
                String sql = template;
                // 1. 替换生成的字段值 (已格式化为 SQL 字符串)
                for (Map.Entry<String, String> entry : formattedValues.entrySet()) {
                    sql = sql.replace("{" + entry.getKey() + "}", entry.getValue());
                }
                // 2. 替换 params 中的原始参数 (如 endDateMonth, bigRegionCode 等)
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    if (entry.getValue() != null) {
                        sql = sql.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
                    }
                }
                sqls.add(sql);
            } else {
                // 模式 2: 顺序替换
                List<Object> argsList = new ArrayList<>();
                for (FieldRule rule : fieldRules) {
                    if (rule.isIncludeInSql()) {
                        argsList.add(formattedValues.get(rule.getName()));
                    }
                }
                Object[] sqlArgs = argsList.toArray();
                sqls.add(String.format(template, sqlArgs));
            }
            
            // 处理额外的 SQL 模板
            if (generatorDefinition.getExtraSqlTemplateKeys() != null) {
                for (String extraTemplateKey : generatorDefinition.getExtraSqlTemplateKeys()) {
                    String extraTemplate = sqlTemplateRepository.getTemplate(extraTemplateKey);
                    if (extraTemplate != null) {
                        // 额外模板强制要求使用命名占位符，因为参数顺序不可知
                        if (extraTemplate.contains("{") && extraTemplate.contains("}")) {
                            String extraSql = extraTemplate;
                            for (Map.Entry<String, String> entry : formattedValues.entrySet()) {
                                extraSql = extraSql.replace("{" + entry.getKey() + "}", entry.getValue());
                            }
                            sqls.add(extraSql);
                        } else {
                            log.warn("Extra SQL template '{}' does not use named placeholders ({{}}). Skipping.", extraTemplateKey);
                        }
                    }
                }
            }
        }
        return sqls;
    }

    private Object generateFieldValue(FieldRule rule, Map<String, Object> recordData, Map<String, Object> params) {
        // 全局覆盖逻辑：如果 params 中包含有效的 endDateMonth，且字段名看起来像日期/月份字段，则优先覆盖
        if (params.containsKey("endDateMonth") && params.get("endDateMonth") != null) {
            String endDateMonth = String.valueOf(params.get("endDateMonth")).trim();
            if (!endDateMonth.isEmpty() && !"null".equalsIgnoreCase(endDateMonth)) {
                String fieldName = rule.getName().toLowerCase();
                // 优先判断是否为“月份”字段：包含 month 且不包含 date/time 的，或者虽然包含 date 但明确是月份含义的
                // 这里的逻辑优化：如果包含 month，我们倾向于保持 YYYY-MM 格式
                if (fieldName.contains("month")) {
                    return endDateMonth;
                }
                // 如果包含 date 或 time，则补全为日期格式
                if (fieldName.contains("date") || fieldName.contains("time") || fieldName.contains("period")) {
                    if (endDateMonth.length() == 7) {
                        return endDateMonth + "-01";
                    }
                    return endDateMonth;
                }
            }
        }

        if (rule instanceof StaticFieldRule staticRule) {
            if (staticRule.getValue() != null && "NULL".equalsIgnoreCase(staticRule.getValue())) {
                return null;
            }
            return staticRule.getValue();
        } else if (rule instanceof RandomIntFieldRule randomIntRule) {
            return ThreadLocalRandom.current().nextInt(randomIntRule.getMin(), randomIntRule.getMax() + 1);
        } else if (rule instanceof RandomDoubleFieldRule randomDoubleRule) {
            // 生成 min 到 max 之间的随机浮点数
            double randomVal = ThreadLocalRandom.current().nextDouble(randomDoubleRule.getMin(), randomDoubleRule.getMax());
            if (randomDoubleRule.getPrecision() != null) {
                // 保留指定小数位
                double scale = Math.pow(10, randomDoubleRule.getPrecision());
                return Math.round(randomVal * scale) / scale;
            }
            return randomVal;
        } else if (rule instanceof UuidFieldRule) {
            return UUID.randomUUID().toString().replace("-", "");
        } else if (rule instanceof DateFieldRule dateRule) {
            LocalDate date = generateDateValue(dateRule, params);
            if (dateRule.getFormat() != null && !dateRule.getFormat().isEmpty()) {
                return date.format(java.time.format.DateTimeFormatter.ofPattern(dateRule.getFormat()));
            }
            return date;
        } else if (rule instanceof EnumLookupFieldRule enumLookupRule) {
            return generateEnumLookupValue(enumLookupRule, recordData);
        } else if (rule instanceof ListRandomFieldRule listRandomRule) {
            return randomElement(listRandomRule.getValues());
        } else if (rule instanceof ExpressionFieldRule expressionRule) {
            return expressionEvaluator.evaluate(expressionRule.getExpression(), recordData, params);
        } else if (rule instanceof ConditionalFieldRule conditionalRule) {
            return evaluateConditionalRule(conditionalRule, recordData, params);
        } else if (rule instanceof ReferenceDataFieldRule refDataRule) {
            return generateReferenceDataValue(refDataRule, recordData, params);
        }
        // ... 其他 RuleType 的处理
        return null; // 默认值
    }

    private LocalDate generateDateValue(DateFieldRule dateRule, Map<String, Object> params) {
        LocalDate baseDate = LocalDate.now();
        if ("PARAM".equalsIgnoreCase(dateRule.getBaseDateSource()) && params.containsKey("baseDate")) {
            baseDate = (LocalDate) params.get("baseDate");
        }

        if (dateRule.getDefaultOffset() != null) {
            baseDate = baseDate.plus(Period.parse(dateRule.getDefaultOffset()));
        }
        if (dateRule.getRandomOffsetDaysMin() != null && dateRule.getRandomOffsetDaysMax() != null) {
            int offset = ThreadLocalRandom.current().nextInt(dateRule.getRandomOffsetDaysMin(), dateRule.getRandomOffsetDaysMax() + 1);
            baseDate = baseDate.plusDays(offset);
        }
        return baseDate;
    }

    private Object generateEnumLookupValue(EnumLookupFieldRule enumLookupRule, Map<String, Object> recordData) {
        List<EnumMapping> mappings = referenceDataManager.getEnumMappings(enumLookupRule.getCategory());
        if (mappings.isEmpty()) {
            log.warn("No enum mappings found for category: {}", enumLookupRule.getCategory());
            return null;
        }

        if (enumLookupRule.getDependsOn() != null) {
            // 如果依赖其他字段，则根据依赖字段的值进行查找
            Object dependentValue = recordData.get(enumLookupRule.getDependsOn());
            if (dependentValue == null) {
                throw new DependencyNotMetException("Dependent field '" + enumLookupRule.getDependsOn() + "' not yet generated.");
            }
            if (enumLookupRule.getLookupBy().equalsIgnoreCase("CODE")) {
                return referenceDataManager.getEnumByCode(enumLookupRule.getCategory(), String.valueOf(dependentValue))
                        .map(m -> enumLookupRule.getTargetField().equalsIgnoreCase("CODE") ? m.getCode() : m.getName())
                        .orElse(null);
            } else { // lookupBy NAME
                return referenceDataManager.getEnumByName(enumLookupRule.getCategory(), String.valueOf(dependentValue))
                        .map(m -> enumLookupRule.getTargetField().equalsIgnoreCase("CODE") ? m.getCode() : m.getName())
                        .orElse(null);
            }
        } else {
            // 否则随机选择一个
            EnumMapping selected = randomElement(mappings);
            return enumLookupRule.getTargetField().equalsIgnoreCase("CODE") ? selected.getCode() : selected.getName();
        }
    }

    private Object evaluateConditionalRule(ConditionalFieldRule conditionalRule, Map<String, Object> recordData, Map<String, Object> params) {
        for (Condition condition : conditionalRule.getConditions()) {
            if (condition.getIfExpression() != null && !condition.getIfExpression().isEmpty()) {
                Object conditionResult = expressionEvaluator.evaluate(condition.getIfExpression(), recordData, params);
                if (conditionResult instanceof Boolean && (Boolean) conditionResult) {
                    return expressionEvaluator.evaluate(condition.getThenValue(), recordData, params);
                }
            } else { // 最后一个 else
                return expressionEvaluator.evaluate(condition.getElseValue(), recordData, params);
            }
        }
        return null; // Should not happen if last condition has an else
    }

    private Object generateReferenceDataValue(ReferenceDataFieldRule refDataRule, Map<String, Object> recordData, Map<String, Object> params) {
        if ("CustMgrData".equalsIgnoreCase(refDataRule.getRefDataType())) {
            String bigRegionCode = null;
            if ("param.bigRegionCode".equals(refDataRule.getLookupValueSource())) {
                bigRegionCode = (String) params.get("bigRegionCode");
            } else if ("RANDOM".equals(refDataRule.getLookupValueSource())) {
                // 随机选择一个大区
                List<String> allBigRegions = referenceDataManager.getAllCustMgrs().stream()
                        .map(CustMgrData::getCustMgrBigRegionCode).distinct().toList();
                bigRegionCode = randomElement(allBigRegions);
            } else {
                // 也可以从 recordData 中获取
                Object val = recordData.get(refDataRule.getLookupValueSource());
                if (val != null) bigRegionCode = String.valueOf(val);
            }

            List<CustMgrData> custMgrs = bigRegionCode != null
                    ? referenceDataManager.getCustMgrsByBigRegion(bigRegionCode)
                    : referenceDataManager.getAllCustMgrs(); // 如果没有指定大区，则从所有客户经理中选

            CustMgrData selectedCustMgr = randomElement(custMgrs);
            if (selectedCustMgr == null) {
                if (refDataRule.isNullable()) {
                    return null;
                } else if (refDataRule.getDefaultNotFoundValue() != null) {
                    return expressionEvaluator.evaluate(refDataRule.getDefaultNotFoundValue(), recordData, params);
                } else {
                    log.warn("No customer manager found for lookup. Using fallback UUID for {}.", refDataRule.getName());
                    return UUID.randomUUID().toString().replace("-", ""); // Fallback
                }
            }
            return expressionEvaluator.evaluate("lookupCustMgrField('" + selectedCustMgr.getCustMgrId() + "', '" + refDataRule.getFieldToPopulate() + "')", recordData, params);

        } else if ("CustomerData".equalsIgnoreCase(refDataRule.getRefDataType())) {
            // Case 1: Lookup ID by Type (for customer_id field)
            if ("type".equalsIgnoreCase(refDataRule.getLookupKeyField())) {
                String targetType = refDataRule.getLookupValueSource(); // e.g., "1"
                // Try to resolve from recordData first
                if (recordData.containsKey(targetType)) {
                    Object val = recordData.get(targetType);
                    if (val != null) targetType = String.valueOf(val);
                }

                List<CustomerData> customers = referenceDataManager.getCustomersByType(targetType);
                CustomerData selected = randomElement(customers);
                if (selected != null) {
                    if ("id".equalsIgnoreCase(refDataRule.getFieldToPopulate())) {
                        return selected.getCustomerId();
                    } else if ("name".equalsIgnoreCase(refDataRule.getFieldToPopulate())) {
                        return selected.getCustomerName();
                    } else if ("manageId".equalsIgnoreCase(refDataRule.getFieldToPopulate())) {
                        return selected.getCustomerManageId();
                    }
                }
                // Fallback
                if (refDataRule.getDefaultNotFoundValue() != null) {
                    return refDataRule.getDefaultNotFoundValue();
                }
                return null;
            }
            // Case 2: Lookup Name by ID (for customer_name field)
            else if ("id".equalsIgnoreCase(refDataRule.getLookupKeyField())) {
                String custId = (String) recordData.get(refDataRule.getLookupValueSource());
                if (custId == null) {
                    throw new DependencyNotMetException("Dependent field '" + refDataRule.getLookupValueSource() + "' not yet generated.");
                }
                return referenceDataManager.getCustomerById(custId)
                        .map(CustomerData::getCustomerName)
                        .orElse(null);
            } else if ("manageId".equalsIgnoreCase(refDataRule.getLookupKeyField())) {
                 String custId = (String) recordData.get(refDataRule.getLookupValueSource());
                 if (custId == null) {
                     throw new DependencyNotMetException("Dependent field '" + refDataRule.getLookupValueSource() + "' not yet generated.");
                 }
                 return referenceDataManager.getCustomerById(custId)
                         .map(CustomerData::getCustomerManageId)
                         .orElse(null);
            }
        }
        // ... 其他引用数据类型
        return null;
    }

    protected <T> T randomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(list.size()));
    }
}
