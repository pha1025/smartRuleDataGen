package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;

@Data
public class Condition {
    private String ifExpression; // 条件表达式，如 "renew_type_code == 'unRenewed'"
    private String thenValue;    // 满足条件时的值
    private String elseValue;    // 不满足条件时的值 (最后一个条件可以只定义 else)
}
