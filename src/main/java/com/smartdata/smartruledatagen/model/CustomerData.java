package com.smartdata.smartruledatagen.model;

import lombok.Data;

@Data
public class CustomerData {
    private String customerId;
    private String customerName;
    private String customerType; // 1, 2, 3
    private String customerManageId; // 关联的客户经理ID
    private String servyouNum;
    private String saleAreaId;
}
