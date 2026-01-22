package com.smartdata.smartruledatagen.model.rules;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RandomDoubleFieldRule extends FieldRule {
    private int min;
    private int max;
}
