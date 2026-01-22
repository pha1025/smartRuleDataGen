package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;

import java.util.Map;

@Data
public class RuleConfig {
    private Map<String, GeneratorDefinition> generators;
}
