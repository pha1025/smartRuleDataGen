package com.smartdata.smartruledatagen.generator.util;

import com.smartdata.smartruledatagen.generator.exception.DependencyNotMetException;
import com.smartdata.smartruledatagen.model.CustMgrData;
import com.smartdata.smartruledatagen.model.CustomerData;
import com.smartdata.smartruledatagen.service.ReferenceDataManager;
import com.smartdata.smartruledatagen.util.DateUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;

import java.util.ArrayList;

@Slf4j
public class ExpressionEvaluator {
    private final ReferenceDataManager referenceDataManager;
    private final Random random = new Random();
    private static final DateTimeFormatter ORDER_NO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DETAIL_ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss"); // Same for now

    public ExpressionEvaluator(ReferenceDataManager referenceDataManager) {
        this.referenceDataManager = referenceDataManager;
    }

    public Object evaluate(String expression, Map<String, Object> recordData, Map<String, Object> params) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }

        // 1. 尝试解析为算术表达式
        if (isArithmeticExpression(expression)) {
            return evaluateArithmetic(expression, recordData, params);
        }

        // 2. 尝试解析为比较表达式
        if (isComparisonExpression(expression)) {
            return evaluateComparison(expression, recordData, params);
        }

        // 尝试解析为固定值
        if (expression.startsWith("'") && expression.endsWith("'")) {
            return expression.substring(1, expression.length() - 1);
        }
        if ("NULL".equalsIgnoreCase(expression)) {
            return null;
        }
        try {
            return Integer.parseInt(expression);
        } catch (NumberFormatException e) {
            // Not an integer, continue
        }
        try {
            return Double.parseDouble(expression);
        } catch (NumberFormatException e) {
            // Not a double, continue
        }
        if ("true".equalsIgnoreCase(expression) || "false".equalsIgnoreCase(expression)) {
            return Boolean.parseBoolean(expression);
        }

        // 尝试解析为字段引用
        if (recordData.containsKey(expression)) {
            return recordData.get(expression);
        }

        // 尝试解析为参数引用
        if (expression.startsWith("param.")) {
            String paramName = expression.substring("param.".length());
            if (params.containsKey(paramName)) {
                return params.get(paramName);
            }
        }

        // 尝试解析为函数调用
        if (expression.contains("(")) {
            Pattern funcPattern = Pattern.compile("(\\w+)\\((.*)\\)");
            Matcher funcMatcher = funcPattern.matcher(expression);
            if (funcMatcher.find()) {
                String funcName = funcMatcher.group(1);
                String argsStr = funcMatcher.group(2);
                
                // 使用 parseArgs 替代简单的 split
                List<String> argsList = parseArgs(argsStr);
                String[] args = argsList.toArray(new String[0]);

                switch (funcName) {
                    case "getParamOrDefault":
                        // Args: paramName, defaultExpression
                        if (args.length == 2) {
                            String paramKey = args[0].trim();
                            // 去除可能的引号
                            if (paramKey.startsWith("'") && paramKey.endsWith("'")) {
                                paramKey = paramKey.substring(1, paramKey.length() - 1);
                            }
                            
                            // 检查 params 中是否有该 key (直接匹配 key，不需要 param. 前缀)
                            if (params.containsKey(paramKey) && params.get(paramKey) != null) {
                                return params.get(paramKey);
                            } else {
                                // 如果没有，评估默认表达式
                                return evaluate(args[1].trim(), recordData, params);
                            }
                        }
                        return null;
                    case "randomInt":
                        // ... (rest of the switch cases)
                        if (args.length == 2) {
                            int min = (int) evaluate(args[0], recordData, params);
                            int max = (int) evaluate(args[1], recordData, params);
                            return ThreadLocalRandom.current().nextInt(min, max + 1);
                        }
                        break;
                    case "randomDateInCurrentMonth":
                        LocalDate now = LocalDate.now();
                        int dayOfMonth = now.getDayOfMonth();
                        int randomDay = ThreadLocalRandom.current().nextInt(1, dayOfMonth + 1);
                        return now.withDayOfMonth(randomDay);
                    case "randomDateTimeBeforeNow":
                        // Args: format (optional), maxDaysBack (optional)
                        int maxDays = 365;
                        if (args.length >= 2 && !args[1].isEmpty()) {
                            maxDays = ((Number) evaluate(args[1], recordData, params)).intValue();
                        }
                        int daysBack = ThreadLocalRandom.current().nextInt(0, maxDays + 1);
                        int hours = ThreadLocalRandom.current().nextInt(0, 24);
                        int minutes = ThreadLocalRandom.current().nextInt(0, 60);
                        int seconds = ThreadLocalRandom.current().nextInt(0, 60);
                        
                        java.time.LocalDateTime randomDateTime = java.time.LocalDateTime.now()
                                .minusDays(daysBack)
                                .withHour(hours)
                                .withMinute(minutes)
                                .withSecond(seconds);
                                
                        if (args.length >= 1 && !args[0].isEmpty()) {
                            String format = (String) evaluate(args[0], recordData, params);
                            return randomDateTime.format(java.time.format.DateTimeFormatter.ofPattern(format));
                        }
                        return randomDateTime;
                    case "getCurrentMonth":
                         // 支持可选参数：offset (months)
                         int offset = 0;
                         if (args.length > 0 && !args[0].isEmpty()) {
                             Object offsetObj = evaluate(args[0], recordData, params);
                             if (offsetObj instanceof Number) {
                                 offset = ((Number) offsetObj).intValue();
                             }
                         }
                         return DateUtil.formatMonth(LocalDate.now().plusMonths(offset));
                    case "randomMonth":
                        if (args.length == 2) {
                            int min = ((Number) evaluate(args[0], recordData, params)).intValue();
                            int max = ((Number) evaluate(args[1], recordData, params)).intValue();
                            int monthOffset = ThreadLocalRandom.current().nextInt(min, max + 1);
                            return DateUtil.formatMonth(LocalDate.now().plusMonths(monthOffset));
                        }
                        return null;
                    case "randomDateInMonth":
                        if (args.length == 1) {
                            Object monthObj = evaluate(args[0], recordData, params);
                            if (monthObj instanceof String monthStr) {
                                // 格式 yyyy-MM
                                java.time.YearMonth ym = java.time.YearMonth.parse(monthStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                                int day = ThreadLocalRandom.current().nextInt(1, ym.lengthOfMonth() + 1);
                                return ym.atDay(day);
                            }
                        }
                        return null;
                    case "randomCustMgrInRegion":
                        // Args: regionCode, fieldName
                        if (args.length == 2) {
                            String regionCode = (String) evaluate(args[0], recordData, params);
                            String fieldName = (String) evaluate(args[1], recordData, params);
                            
                            List<CustMgrData> mgrs = referenceDataManager.getCustMgrsByBigRegion(regionCode);
                            if (mgrs != null && !mgrs.isEmpty()) {
                                CustMgrData randomMgr = mgrs.get(random.nextInt(mgrs.size()));
                                return getCustMgrField(randomMgr, fieldName);
                            }
                        }
                        return null;
                    case "generateDetailOrderId":
                        // 格式：yyyyMMddHHmmss + 15位随机数 (共29位)
                        // 示例：20250917001148711015020000045
                        String detailDateStr = java.time.LocalDateTime.now().format(DETAIL_ORDER_NO_FORMAT);
                        StringBuilder detailSb = new StringBuilder(detailDateStr);
                        for (int i = 0; i < 15; i++) {
                            detailSb.append(random.nextInt(10));
                        }
                        return detailSb.toString();
                    case "lookupProvinceByRegion":
                        if (args.length == 1) {
                            String regionCode = (String) evaluate(args[0], recordData, params);
                            return referenceDataManager.getProvinceCodeByBigRegion(regionCode);
                        }
                        return "440000"; // default
                    case "generateOrderNo":
                        // 生成 yyyyMMddHHmmss + 15位随机数
                        String dateStr = java.time.LocalDateTime.now().format(ORDER_NO_DATE_FORMAT);
                        StringBuilder sb = new StringBuilder(dateStr);
                        for (int i = 0; i < 15; i++) {
                            sb.append(random.nextInt(10));
                        }
                        return sb.toString();
                    case "randomBoolean":
                        if (args.length == 1) {
                            double probability = (double) evaluate(args[0], recordData, params);
                            return random.nextDouble() < probability;
                        }
                        break;
                    case "lookupCustMgrField":
                        if (args.length == 2) {
                            Object custMgrIdObj = evaluate(args[0], recordData, params);
                            String fieldName = (String) evaluate(args[1], recordData, params);
                            if (custMgrIdObj instanceof String custMgrId) {
                                Optional<CustMgrData> custMgrData = referenceDataManager.getCustMgrById(custMgrId);
                                if (custMgrData.isPresent()) {
                                    return getCustMgrField(custMgrData.get(), fieldName);
                                }
                            }
                        }
                        return null;
                    case "lookupCustomerField":
                        if (args.length == 2) {
                            Object customerIdObj = evaluate(args[0], recordData, params);
                            String fieldName = (String) evaluate(args[1], recordData, params);
                            if (customerIdObj instanceof String customerId) {
                                Optional<CustomerData> customerData = referenceDataManager.getCustomerById(customerId);
                                if (customerData.isPresent()) {
                                    return getCustomerField(customerData.get(), fieldName);
                                }
                            }
                        }
                        return null;
                    case "lookupEnumCode":
                    case "lookupEnumName":
                        if (args.length == 2) {
                            String category = (String) evaluate(args[0], recordData, params);
                            Object valueToLookup = evaluate(args[1], recordData, params);
                            if (valueToLookup instanceof String lookupValue) {
                                if ("RANDOM".equalsIgnoreCase(lookupValue)) {
                                    List<com.smartdata.smartruledatagen.model.EnumMapping> mappings = referenceDataManager.getEnumMappings(category);
                                    if (mappings != null && !mappings.isEmpty()) {
                                        com.smartdata.smartruledatagen.model.EnumMapping selected = mappings.get(random.nextInt(mappings.size()));
                                        return "lookupEnumCode".equals(funcName) ? selected.getCode() : selected.getName();
                                    }
                                    return null;
                                }
                                if ("lookupEnumCode".equals(funcName)) {
                                    return referenceDataManager.getEnumByName(category, lookupValue)
                                            .map(e -> e.getCode())
                                            .orElse(null);
                                } else { // lookupEnumName
                                    return referenceDataManager.getEnumByCode(category, lookupValue)
                                            .map(e -> e.getName())
                                            .orElse(null);
                                }
                            }
                        }
                        return null;
                    case "formatMonth":
                    case "formatDate":
                        if (args.length == 1) {
                            Object dateObj = evaluate(args[0], recordData, params);
                            LocalDate date = null;
                            if (dateObj instanceof LocalDate) {
                                date = (LocalDate) dateObj;
                            } else if (dateObj instanceof String) {
                                try {
                                    // 尝试解析 yyyy-MM-dd
                                    date = LocalDate.parse((String) dateObj);
                                } catch (Exception e) {
                                    // ignore
                                }
                            }
                            
                            if (date != null) {
                                return "formatMonth".equals(funcName) ? DateUtil.formatMonth(date) : DateUtil.formatDate(date);
                            }
                        }
                        return null;
                    case "lastDayOfMonth":
                        // Args: optionalMonthStr (yyyy-MM)
                        LocalDate dateForLastDay = LocalDate.now();
                        if (args.length >= 1) {
                            try {
                                Object monthObj = evaluate(args[0], recordData, params);
                                if (monthObj instanceof String && !((String) monthObj).isEmpty() && !"null".equalsIgnoreCase((String)monthObj)) {
                                    java.time.YearMonth ym = java.time.YearMonth.parse((String) monthObj, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                                    dateForLastDay = ym.atEndOfMonth();
                                    return DateUtil.formatDate(dateForLastDay);
                                }
                            } catch (Exception e) {
                                // If param not found or invalid, fallback to current month
                                log.warn("Failed to evaluate month for lastDayOfMonth, using current month. Error: {}", e.getMessage());
                            }
                        }
                        // Default to current month's last day
                        return DateUtil.formatDate(dateForLastDay.withDayOfMonth(dateForLastDay.lengthOfMonth()));
                    // TODO: Add more helper functions as needed
                }
            }
        }

        // 尝试处理简单的字符串连接和字段引用
        // 示例: "'测试客户_' + customer_id.substring(0, 8)"
        // 这部分会非常复杂，建议使用一个真正的脚本引擎或限制表达式的复杂性
        // 对于这个示例，我们只支持简单字段引用和字符串拼接
        if (expression.contains("+") || expression.contains("substring")) {
            // 简单解析 'xxx' + field + 'yyy' 或 field.substring(...)
            String result = expression;
            for (Map.Entry<String, Object> entry : recordData.entrySet()) {
                result = result.replace(entry.getKey(), String.valueOf(entry.getValue()));
            }
            // 替换参数
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                result = result.replace("param." + entry.getKey(), String.valueOf(entry.getValue()));
            }
            // 移除字符串引号，进行连接（非常原始的实现）
            result = result.replace("'", "");
            result = result.replace(" ", ""); // 移除空格
            // 如果还包含 + 符号，尝试拼接
            if (result.contains("+")) {
                StringBuilder finalResult = new StringBuilder();
                for (String part : result.split("\\+")) {
                    finalResult.append(part);
                }
                return finalResult.toString();
            }
            // 如果包含 substring，尝试简单执行
            if (result.contains(".substring(")) {
                Pattern substringPattern = Pattern.compile("(\\w+)\\.substring\\((\\d+)(,\\s*(\\d+))?\\)");
                Matcher substringMatcher = substringPattern.matcher(result);
                if (substringMatcher.find()) {
                    String target = substringMatcher.group(1); // 目标变量名
                    int start = Integer.parseInt(substringMatcher.group(2));
                    Integer end = substringMatcher.group(4) != null ? Integer.parseInt(substringMatcher.group(4)) : null;

                    Object targetValue = recordData.get(target);
                    if (targetValue instanceof String strValue) {
                        return end != null ? strValue.substring(start, end) : strValue.substring(start);
                    }
                }
            }
            return result; // 简化处理，直接返回替换后的字符串
        }

        log.debug("Could not evaluate expression: {}. Throwing DependencyNotMetException.", expression);
        throw new DependencyNotMetException("Dependency not met for expression: " + expression);
    }


    private List<String> parseArgs(String argsStr) {
        List<String> args = new ArrayList<>();
        if (argsStr == null || argsStr.trim().isEmpty()) {
            return args;
        }

        int parenthesisCount = 0;
        StringBuilder currentArg = new StringBuilder();

        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);
            if (c == '(') {
                parenthesisCount++;
            } else if (c == ')') {
                parenthesisCount--;
            }

            if (c == ',' && parenthesisCount == 0) {
                args.add(currentArg.toString().trim());
                currentArg.setLength(0);
            } else {
                currentArg.append(c);
            }
        }
        if (currentArg.length() > 0) {
            args.add(currentArg.toString().trim());
        }
        return args;
    }

    private Object getCustMgrField(CustMgrData data, String fieldName) {
        try {
            // 使用反射获取 CustMgrData 的字段值
            // Lombok 生成的 getter 方法名是 getFieldName()
            String getterMethodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method method = CustMgrData.class.getMethod(getterMethodName);
            return method.invoke(data);
        } catch (Exception e) {
            log.error("Failed to get field {} from CustMgrData using reflection.", fieldName, e);
            return null;
        }
    }

    private Object getCustomerField(CustomerData data, String fieldName) {
        try {
            // 使用反射获取 CustomerData 的字段值
            String getterMethodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            Method method = CustomerData.class.getMethod(getterMethodName);
            return method.invoke(data);
        } catch (Exception e) {
            log.error("Failed to get field {} from CustomerData using reflection.", fieldName, e);
            return null;
        }
    }

    private boolean isArithmeticExpression(String expression) {
        // 排除掉函数调用中的参数分隔符，这里简单假设算术运算不包含在引号内
        // 且必须包含 + 或 -，但不是开头（负数）
        // 这是一个非常简陋的检查
        if (expression.startsWith("'")) return false;
        // 忽略 randomInt(-100, 100) 中的负号
        // 简单策略：如果存在 " + " 或 " - " (前后有空格)，则认为是算术运算
        return expression.contains(" + ") || expression.contains(" - ");
    }

    private boolean isComparisonExpression(String expression) {
        return expression.contains(" == ") || expression.contains(" != ");
    }

    private Boolean evaluateComparison(String expression, Map<String, Object> recordData, Map<String, Object> params) {
        String operator = expression.contains(" == ") ? " == " : " != ";
        String[] parts = expression.split(operator, 2);
        if (parts.length != 2) return false;

        Object leftVal = evaluate(parts[0].trim(), recordData, params);
        Object rightVal = evaluate(parts[1].trim(), recordData, params);

        String leftStr = String.valueOf(leftVal);
        String rightStr = String.valueOf(rightVal);

        boolean isEqual = leftStr.equals(rightStr);
        return " == ".equals(operator) ? isEqual : !isEqual;
    }

    private Object evaluateArithmetic(String expression, Map<String, Object> recordData, Map<String, Object> params) {
        // 简单分割，不支持混合运算顺序，从左到右
        // 这里的实现仅支持 A + B 或 A - B
        String operator = expression.contains(" + ") ? "\\+" : "-";
        String splitRegex = expression.contains(" + ") ? " \\+ " : " - ";
        
        String[] parts = expression.split(splitRegex, 2); // 只分割第一个运算符
        if (parts.length != 2) return null;
        
        Object leftVal = evaluate(parts[0].trim(), recordData, params);
        Object rightVal = evaluate(parts[1].trim(), recordData, params);
        
        if (leftVal instanceof Number && rightVal instanceof Number) {
            double l = ((Number) leftVal).doubleValue();
            double r = ((Number) rightVal).doubleValue();
            double result = expression.contains(" + ") ? l + r : l - r;
            
            // 如果两个都是整数，返回整数
            if (leftVal instanceof Integer && rightVal instanceof Integer) {
                return (int) result;
            }
            return result;
        }
        return null;
    }
}
