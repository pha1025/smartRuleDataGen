package com.smartdata.smartruledatagen.model;

import lombok.Data;

@Data
public class EnumMapping {
    private String category; // 例如 "expect_renew_type" 或 "order_type"
    private String code;     // 例如 "lose" 或 "qyzy"
    private String name;     // 例如 "流失风险" 或 "企业会员普通单"
}
