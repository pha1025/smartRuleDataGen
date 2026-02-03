package com.smartdata.smartruledatagen.dto;

import lombok.Data;

@Data
public class ConsumePtsRequest {
    private Long customer_id;
    private String customer_name;
    private int companyNum;
    private int deptNum;
    private int employeeNum;
    private String statistics_month;
    private boolean executeInsert;
}
