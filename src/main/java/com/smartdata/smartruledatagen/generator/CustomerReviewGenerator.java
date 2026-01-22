package com.smartdata.smartruledatagen.generator;

import com.smartdata.smartruledatagen.model.CustMgrData;
import com.smartdata.smartruledatagen.model.EnumMapping;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("customerReviewExpireGenerator") // 指定一个Bean名称
public class CustomerReviewGenerator extends AbstractTableGenerator {

    @Override
    public List<String> generateSqls(int count, Map<String, Object> params) {
        List<String> sqls = new ArrayList<>();
        String template = sqlTemplateRepository.getTemplate("customer_review_expire");

        // 默认日期
        LocalDate baseDate = (LocalDate) params.getOrDefault("baseDate", LocalDate.now().plusMonths(3));
        String targetBigRegionCode = (String) params.get("bigRegionCode"); // 可选参数：指定大区

        List<EnumMapping> renewTypeMappings = referenceDataManager.getEnumMappings("renew_type");
        List<EnumMapping> expectRenewTypeMappings = referenceDataManager.getEnumMappings("expect_renew_type");

        for (int i = 0; i < count; i++) {
            LocalDate endDate = baseDate.plusDays(randomInt(-15, 15)); // 随机日期浮动
            String endDateMonth = formatMonth(endDate);

            String customerId = generateUUID();
            String customerName = "测试客户_" + customerId.substring(0, 8);
            String outletCode = "004012020002"; // 示例值
            String provinceCityAreaCode = "440000"; // 示例值

            // --- 期望续费类型 (expect_renew_type_code, expect_renew_type) ---
            EnumMapping expectRenewType = randomElement(expectRenewTypeMappings);
            String expectRenewTypeCode = expectRenewType != null ? expectRenewType.getCode() : "lose";
            String expectRenewTypeName = expectRenewType != null ? expectRenewType.getName() : "流失风险";

            // --- 续费类型 (renew_type_code, renew_type) 及 is_renew ---
            EnumMapping renewType = randomElement(renewTypeMappings);
            String renewTypeCode = renewType != null ? renewType.getCode() : "unRenewed";
            String renewTypeName = renewType != null ? renewType.getName() : "未续费";
            String isRenewCode = (renewTypeCode.equals("unRenewed") || renewTypeCode.equals("reduced")) ? "N" : "Y"; // 根据 renew_type_code 联动

            int curPackagePaidAmount = randomInt(500, 5000);
            int realPackageSalesAmt = curPackagePaidAmount + randomInt(-100, 100);
            // renew_package_sales_amount 可以为 null 或随机值
            String renewPackageSalesAmount = random.nextBoolean() ? quote(randomInt(500, 6000)) : quote((String) null);

            String packageLevel = randomElement(List.of("基础版", "专业版", "旗舰版"));
            String actionNames = randomElement(List.of("智能办税", "风控", "报销", "发票管理", "智能办税,风控"));
            String renewPackageLevel = packageLevel; // 简单起见，假设续费级别不变

            int avgApplicantsNum = randomInt(0, 10);
            int inInvoiceAvgAmt = randomInt(0, 1000);
            int outInvoiceAvgAmt = randomInt(0, 1000);
            String isComplianceTaxTarget = random.nextBoolean() ? "Y" : "N";
            String activeComplianceStatus = isComplianceTaxTarget.equals("Y") ? "compliance" : "notCompliance";

            // --- 客户经理信息 (cust_mgr_id, cust_mgr_name, big_region_code, etc.) ---
            CustMgrData custMgr = null;
            String custMgrBigRegionCode = null;
            String custMgrBigRegionName = null;
            if (targetBigRegionCode != null) {
                // 如果指定了大区，则从指定大区中选择客户经理
                List<CustMgrData> custMgrsInRegion = referenceDataManager.getCustMgrsByBigRegion(targetBigRegionCode);
                custMgr = randomElement(custMgrsInRegion);
            } else {
                // 否则从所有客户经理中随机选择
                List<CustMgrData> allCustMgrs = referenceDataManager.getCustMgrsByBigRegion("004011006002"); // 示例，需要从所有大区中选择
                // 更合理的做法是从 referenceDataManager.custMgrDataById.values() 中随机选择
                // 这里为了简化，假设 '004011006002' 是一个可用的示例大区
                if (!allCustMgrs.isEmpty()) {
                    custMgr = randomElement(allCustMgrs);
                }
            }

            String custMgrId = custMgr != null ? custMgr.getCustMgrId() : generateUUID(); // 如果没找到， fallback到一个UUID
            String custMgrName = custMgr != null ? custMgr.getCustMgrName() : "默认客户经理";
            custMgrBigRegionCode = custMgr != null ? custMgr.getCustMgrBigRegionCode() : "000000000"; // 默认或随机
            custMgrBigRegionName = custMgr != null ? custMgr.getCustMgrBigRegionName() : "默认大区";


            String lastestFollowTime = quote((String) null); // 可为 NULL
            String followType = "";
            String followTypeName = "";
            String followProgress = "";
            String followProgressName = "";
            String followIntention = "";
            String followIntentionName = "";
            String lastestPaymentMethodName = quote((String) null);
            String lastestPaymentTime = quote((String) null);
            String lastestOrderTime = quote((String) null);
            String isOrder = "Y";

            String sql = String.format(template,
                    endDateMonth, formatDate(endDate), customerId, customerName, outletCode,
                    expectRenewTypeCode, expectRenewTypeName, isRenewCode, isRenewCode.equals("Y") ? "已续费" : "未续费", // is_renew
                    curPackagePaidAmount, realPackageSalesAmt, renewPackageSalesAmount, packageLevel, actionNames, renewPackageLevel,
                    avgApplicantsNum, inInvoiceAvgAmt, outInvoiceAvgAmt, isComplianceTaxTarget, activeComplianceStatus,
                    lastestFollowTime, followType, followTypeName, followProgress, followProgressName, followIntention, followIntentionName,
                    lastestPaymentMethodName, lastestPaymentTime, lastestOrderTime,
                    renewTypeName, renewTypeCode, custMgrId, custMgrName, provinceCityAreaCode, custMgrBigRegionCode, isOrder
            );
            sqls.add(sql);
        }
        return sqls;
    }

    @Override
    public String getDbKey() {
        return "db2"; // 这个生成器使用 db2 数据库
    }
}
