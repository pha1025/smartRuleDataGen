package com.smartdata.smartruledatagen.generator.util;

import com.smartdata.smartruledatagen.util.DateUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ValueFormatter {
    public static String formatForSql(Object value, String sqlType, boolean nullable) {
        if (value == null) {
            return nullable ? "NULL" : "''"; // 如果允许NULL则返回"NULL"，否则返回空字符串
        }

        if (sqlType == null) {
            // 如果未指定类型，尝试根据对象类型推断或默认按字符串处理
            if (value instanceof Number) return value.toString();
            if (value instanceof Boolean) return (Boolean) value ? "1" : "0";
            return "'" + value.toString().replace("'", "''") + "'";
        }

        switch (sqlType.toUpperCase()) {
            case "VARCHAR":
            case "TEXT":
            case "CHAR":
                return "'" + value.toString().replace("'", "''") + "'"; // 字符串加引号并转义单引号
            case "INT":
            case "BIGINT":
            case "DECIMAL":
            case "NUMERIC":
            case "FLOAT":
            case "DOUBLE":
                return value.toString(); // 数字直接返回
            case "DATE":
                if (value instanceof LocalDate) {
                    return "'" + DateUtil.formatDate((LocalDate) value) + "'";
                }
                return "'" + value.toString() + "'"; // 尝试直接转字符串
            case "DATETIME":
            case "TIMESTAMP":
                if (value instanceof LocalDateTime) {
                    return "'" + ((LocalDateTime) value).format(DateUtil.DATETIME_FORMATTER) + "'";
                } else if (value instanceof LocalDate) { // LocalDate 也可以格式化成 DATETIME
                    return "'" + ((LocalDate) value).atStartOfDay().format(DateUtil.DATETIME_FORMATTER) + "'";
                }
                return "'" + value.toString() + "'";
            case "BOOLEAN":
                // 根据数据库类型可能需要转换为 'Y'/'N' 或 1/0
                return (Boolean) value ? "1" : "0";
            default:
                // 默认按字符串处理
                return "'" + value.toString().replace("'", "''") + "'";
        }
    }
}
