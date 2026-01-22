package com.smartdata.smartruledatagen.dto;

import lombok.Data;

@Data
public class DataGenRequest {
    private String generatorName;
    private int count;
    private String regionCode;
    private boolean executeInsert;
}
