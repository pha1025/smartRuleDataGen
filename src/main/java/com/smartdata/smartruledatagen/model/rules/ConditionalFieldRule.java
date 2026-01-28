package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConditionalFieldRule extends FieldRule {
    private List<Condition> conditions; // 条件列表
}

