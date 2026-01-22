package com.smartdata.smartruledatagen.model.rules;

public enum RuleType {
    STATIC,         // 固定值
    RANDOM_INT,     // 随机整数
    RANDOM_DOUBLE,  // 随机浮点数
    RANDOM_BOOLEAN, // 随机布尔
    UUID,           // UUID
    DATE,           // 日期 (可带偏移、随机)
    ENUM_LOOKUP,    // 枚举查找 (根据category和code/name)
    LIST_RANDOM,    // 从列表中随机选择
    EXPRESSION,     // 表达式求值 (基于其他已生成字段)
    CONDITIONAL,    // 条件判断
    REFERENCE_DATA  // 引用数据查找 (如客户经理信息)
}
