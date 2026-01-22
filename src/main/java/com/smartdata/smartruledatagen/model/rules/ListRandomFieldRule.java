package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ListRandomFieldRule extends FieldRule {
    private List<String> values; // 可选值列表{
}
