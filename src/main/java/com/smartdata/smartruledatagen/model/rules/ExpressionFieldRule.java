package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExpressionFieldRule extends FieldRule {
    private String expression; // 表达式字符串，如 "'测试客户_' + customer_id.substring(0, 8)"{
}
