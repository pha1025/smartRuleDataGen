package com.smartdata.smartruledatagen.generator;

import com.smartdata.smartruledatagen.model.CustMgrData;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("tradeKpiPaymentGenerator") // 指定一个Bean名称
public class TradeKpiPaymentGenerator extends AbstractTableGenerator {

    @Override
    public List<String> generateSqls(int count, Map<String, Object> params) {
        List<String> sqls = new ArrayList<>();
        String template = sqlTemplateRepository.getTemplate("trade_kpi_payment");

        LocalDate baseDate = (LocalDate) params.getOrDefault("baseDate", LocalDate.now());
        String targetBigRegionCode = (String) params.get("bigRegionCode");

        for (int i = 0; i < count; i++) {
            LocalDate paymentDate = baseDate.plusDays(randomInt(-5, 5));
            String outletCode = "004012020002";
            String outletName = "粤西客成组";
            String incomeType = randomElement(List.of("add", "renew"));
            String tradeType = randomElement(List.of("papTotalIncome", "newIncome"));

            // --- 客户经理信息 ---
            CustMgrData custMgr = null;
            if (targetBigRegionCode != null) {
                List<CustMgrData> custMgrsInRegion = referenceDataManager.getCustMgrsByBigRegion(targetBigRegionCode);
                custMgr = randomElement(custMgrsInRegion);
            } else {
                List<CustMgrData> allCustMgrs = referenceDataManager.getCustMgrsByBigRegion("004011006002"); // 示例
                if (!allCustMgrs.isEmpty()) {
                    custMgr = randomElement(allCustMgrs);
                }
            }

            String custMgrId = custMgr != null ? custMgr.getCustMgrId() : generateUUID();
            String custMgrName = custMgr != null ? custMgr.getCustMgrName() : "默认客户经理";
            String custMgrOutletCode = custMgr != null ? custMgr.getCustMgrOutletCode() : "000000000000";
            String custMgrOutletName = custMgr != null ? custMgr.getCustMgrOutletName() : "默认网点组";
            String custMgrBigRegionCode = custMgr != null ? custMgr.getCustMgrBigRegionCode() : "000000000";
            String custMgrBigRegionName = custMgr != null ? custMgr.getCustMgrBigRegionName() : "默认大区";

            // income_khjl 字段通常和 cust_mgr 类似，这里简化为一致
            String incomeKhjlId = custMgrId;
            String incomeKhjlName = custMgrName;
            String incomeKhjlOutletCode = custMgrOutletCode;
            String incomeKhjlOutletName = custMgrOutletName;
            String incomeKhjlBigRegionCode = custMgrBigRegionCode;
            String incomeKhjlBigRegionName = custMgrBigRegionName;

            int zxPaymentAmount = randomInt(100, 5000);
            int zxGroupPaymentAmount = randomInt(0, zxPaymentAmount);

            // --- group_id 逻辑 (根据 cust_mgr_big_region_code = '004012022' 判断) ---
            String groupId = quote((String) null);
            String groupName = quote((String) null);
            // 假设 '004012022' 是您在示例1中提到的触发 group_id 查找的大区
            if ("004012022".equals(custMgrBigRegionCode)) {
                // 此时需要根据 custMgrId 从另一个表 cust_group_relation_hb_sr 查 group_id
                // 暂时假设这个 lookup 也在 ReferenceDataManager 里
                // 例如: Optional<String> foundGroupId = referenceDataManager.getGroupIdByCustMgrId(custMgrId);
                // if (foundGroupId.isPresent()) {
                //     groupId = quote(foundGroupId.get());
                //     groupName = quote("对应组名"); // 假设也能查到组名
                // }
                // 简化：这里我们直接从 CustMgrData 中获取，如果 CustMgrData 包含了 group_id
                if (custMgr != null && custMgr.getCustMgrGroupId() != null) {
                    groupId = quote(custMgr.getCustMgrGroupId());
                    groupName = quote(custMgr.getCustMgrGroupName());
                } else {
                    // 如果大区匹配但没查到 group_id, 仍然是 NULL
                    groupId = quote((String) null);
                    groupName = quote((String) null);
                }
            }
            int zxAssessmentPaymentAmount = 0; // 示例

            String sql = String.format(template,
                    formatDate(paymentDate), outletCode, outletName, incomeType, tradeType,
                    custMgrId, custMgrName, custMgrOutletCode, custMgrOutletName,
                    incomeKhjlId, incomeKhjlName, incomeKhjlOutletCode, incomeKhjlOutletName,
                    zxPaymentAmount, zxGroupPaymentAmount, groupId, groupName, zxAssessmentPaymentAmount,
                    custMgrBigRegionCode, custMgrBigRegionName, incomeKhjlBigRegionCode, incomeKhjlBigRegionName
            );
            sqls.add(sql);
        }
        return sqls;
    }

    @Override
    public String getDbKey() {
        return "db1"; // 这个生成器使用 db1 数据库
    }
}
