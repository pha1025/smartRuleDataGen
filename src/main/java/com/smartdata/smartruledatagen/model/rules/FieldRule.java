package com.smartdata.smartruledatagen.model.rules;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type", // YAML中用于区分具体规则类型的字段
        defaultImpl = StaticFieldRule.class // 默认实现，如果'type'字段缺失
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StaticFieldRule.class, name = "STATIC"),
        @JsonSubTypes.Type(value = RandomIntFieldRule.class, name = "RANDOM_INT"),
        @JsonSubTypes.Type(value = RandomDoubleFieldRule.class, name = "RANDOM_DOUBLE"),
        @JsonSubTypes.Type(value = UuidFieldRule.class, name = "UUID"),
        @JsonSubTypes.Type(value = DateFieldRule.class, name = "DATE"),
        @JsonSubTypes.Type(value = EnumLookupFieldRule.class, name = "ENUM_LOOKUP"),
        @JsonSubTypes.Type(value = ListRandomFieldRule.class, name = "LIST_RANDOM"),
        @JsonSubTypes.Type(value = ExpressionFieldRule.class, name = "EXPRESSION"),
        @JsonSubTypes.Type(value = ConditionalFieldRule.class, name = "CONDITIONAL"),
        @JsonSubTypes.Type(value = ReferenceDataFieldRule.class, name = "REFERENCE_DATA")
        // 可以继续添加其他 RuleType 对应的实现
})

@Data
public abstract class FieldRule {
    protected String name; // 字段名
    protected String sqlType; // 字段在SQL中的类型，用于格式化 (e.g., "VARCHAR", "INT", "DATE")
    protected boolean nullable = false; // 是否允许为 NULL
    protected boolean includeInSql = true; // 是否包含在生成的 SQL 语句中（默认为 true）
}
