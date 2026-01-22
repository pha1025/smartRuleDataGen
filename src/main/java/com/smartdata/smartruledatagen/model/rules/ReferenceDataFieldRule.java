package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReferenceDataFieldRule extends FieldRule {
    private String refDataType; // 引用数据的类型，如 "CustMgrData"
    private String lookupKeyField; // 在 ReferenceDataManager 中查找的字段，例如 "bigRegionCode"
    private String lookupValueSource; // 查找值的来源，例如 "param.bigRegionCode" 或 "RANDOM"
    private String fieldToPopulate; // 从查找到的数据对象中取哪个字段的值
    private String defaultNotFoundValue; // 如果没找到时的默认值 (如果 nullable=false)
    private String dependsOn; // 依赖字段，虽然 lookupValueSource 已隐含依赖，但显式声明有助于理解或未来扩展
}
