package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;

import java.util.List;

@Data
public class GeneratorDefinition {
    private String tableName;
    private String dbKey; // 对应的数据库配置key
    private String sqlTemplateKey; // 对应的SQL模板key
    private List<FieldRule> fields; // 字段规则列表
}
