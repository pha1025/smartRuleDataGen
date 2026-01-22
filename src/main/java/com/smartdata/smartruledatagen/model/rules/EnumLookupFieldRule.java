package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EnumLookupFieldRule extends FieldRule {
    private String category; // 枚举类别，如 "expect_renew_type"
    private String lookupBy; // "CODE" or "NAME"
    private String targetField; // "CODE" or "NAME"
    private String dependsOn; // 依赖的字段，例如如果希望根据已知的 code 查找 name
}
