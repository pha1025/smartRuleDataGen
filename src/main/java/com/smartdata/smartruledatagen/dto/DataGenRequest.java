package com.smartdata.smartruledatagen.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DataGenRequest {
    private String generatorName;
    private int count;
    private String regionCode;
    private boolean executeInsert;
    private Map<String, Object> extraParams;
}
