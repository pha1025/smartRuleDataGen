package com.smartdata.smartruledatagen.model;

import lombok.Data; // 使用Lombok自动生成getter/setter/toString

@Data
public class CustMgrData {
    private String custMgrBigRegionCode;
    private String custMgrBigRegionName; // 例如 "销服团队北区"
    private String custMgrOutletCode;    // 例如 "004011006002001"
    private String custMgrOutletName;    // 例如 "张承客成组"
    private String custMgrId;            // 例如 "57266D99C9FB72AD09BFBFF4B8AC0218"
    private String custMgrName;          // 例如 "李莉"
    // 如果 group_id 可以直接从这个Excel中查到，可以加在这里
    private String custMgrGroupId;
    private String custMgrGroupName;
}