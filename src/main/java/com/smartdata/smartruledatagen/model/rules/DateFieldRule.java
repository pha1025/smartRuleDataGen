package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DateFieldRule extends FieldRule {
    private String format; // 日期格式，如 "yyyy-MM-dd"
    private String baseDateSource; // "PARAM" (来自用户输入) 或 "NOW"
    private String defaultOffset; // 默认偏移，如 "P3M" (3个月), "P-10D" (前10天)
    private Integer randomOffsetDaysMin; // 随机天数偏移范围
    private Integer randomOffsetDaysMax;
}
