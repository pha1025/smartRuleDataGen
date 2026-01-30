package com.smartdata.smartruledatagen.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SqlTemplateRepository {

    private final Map<String, String> templates = new HashMap<>();

    public SqlTemplateRepository() {
        // 例1: ads_trade_kpi_zx_order_payment_cust_rt 模板
        // 使用命名占位符 {fieldName} 以支持更灵活的字段映射
        templates.put("trade_kpi_payment",
                "INSERT INTO servyou_ads.ads_trade_kpi_zx_order_payment_cust_rt\n" +
                        "(payment_date, outlet_code, outlet_name, income_type, trade_type, cust_mgr_id, cust_mgr_name, cust_mgr_outlet_code, cust_mgr_outlet_name, income_khjl_id, income_khjl_name, income_khjl_outlet_code, income_khjl_outlet_name, zx_payment_amount, zx_group_payment_amount, group_id, group_name, zx_assessment_payment_amount, cust_mgr_big_region_code, cust_mgr_big_region_name, income_khjl_big_region_code, income_khjl_big_region_name)\n" +
                        "VALUES\n" +
                        "({payment_date}, {outlet_code}, {outlet_name}, {income_type}, {trade_type}, {cust_mgr_id}, {cust_mgr_name}, {cust_mgr_outlet_code}, {cust_mgr_outlet_name}, {income_khjl_id}, {income_khjl_name}, {income_khjl_outlet_code}, {income_khjl_outlet_name}, {zx_payment_amount}, {zx_group_payment_amount}, {group_id}, {group_name}, {zx_assessment_payment_amount}, {cust_mgr_big_region_code}, {cust_mgr_big_region_name}, {income_khjl_big_region_code}, {income_khjl_big_region_name})");

        // 例3: ads_trade_kpi_zx_order_payment_detail_rt 模板 (Generator 3)
        templates.put("trade_kpi_payment_detail",
                "INSERT INTO servyou_ads.ads_trade_kpi_zx_order_payment_detail_rt\n" +
                        "(payment_date, order_id, customer_id, customer_name, customer_type, income_type, trade_type, outlet_code, outlet_name, cust_mgr_id, cust_mgr_name, signer_id, signer_name, income_khjl_id, income_khjl_name, income_khjl_outlet_code, income_khjl_outlet_name, payment_amount, assessment_amount, order_source, payment_method_name, order_type)\n" +
                        "VALUES\n" +
                        "({payment_date}, {order_id}, {customer_id}, {customer_name}, {customer_type}, {income_type}, {trade_type}, {outlet_code}, {outlet_name}, {cust_mgr_id}, {cust_mgr_name}, {signer_id}, {signer_name}, {income_khjl_id_detail}, {income_khjl_name_detail}, {income_khjl_outlet_code}, {income_khjl_outlet_name}, {payment_amount}, {assessment_amount}, {order_source}, {payment_method_name}, {order_type})");

        // 例4: ads_trade_kpi_cs_order_payment_rt 模板 (Generator 4)
        templates.put("cs_order_payment",
                "INSERT INTO cctest_not_delete.ads_trade_kpi_cs_order_payment_rt\n" +
                        "(payment_date, cust_mgr_id, cust_mgr_name, cust_mgr_outlet_code, cust_mgr_outlet_name, cust_mgr_big_region_code, cust_mgr_big_region_name, order_source, kpi_order_source, customer_sort, contract_income_id, join_order_detail_id, order_id, order_detail_id, order_item_id, product_customer_new_type, product_name, payment_amount, order_signer, order_signer_trueid, order_creator, order_creator_trueid, main_customer_id, main_customer_name, product_customer_id,product_customer_name, business_class, business_class_code, order_type, order_type_code)\n" +
                        "VALUES\n" +
                        "(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)");

        // 例2: ads_itcrm_customer_review_zx_product_expire 模板
        templates.put("customer_review_expire",
                "INSERT INTO cctest_not_delete.ads_itcrm_customer_review_zx_product_expire\n" +
                        "(end_date_month, end_date, customer_id, customer_name, outlet_code, expect_renew_type_code, expect_renew_type, is_renew_code, is_renew, cur_package_paid_amount, cur_package_sales_amount, real_package_sales_amt, renew_package_sales_amount, package_level, action_names, renew_package_level, avg_applicants_num, in_invoice_avg_amt, out_invoice_avg_amt, is_compliance_tax_target, active_compliance_status,\n" +
                        "lastest_follow_time, follow_type, follow_type_name, follow_progress, follow_progress_name, follow_intention, follow_intention_name, lastest_payment_method, lastest_payment_method_name, lastest_payment_time, lastest_order_time, renew_type, renew_type_code, cust_mgr_id, cust_mgr_name, province_city_area_code, big_region_code, is_order)\n" +
                        "VALUES\n" +
                        "({end_date_month}, {end_date}, {customer_id}, {customer_name}, {outlet_code}, {expect_renew_type_code}, {expect_renew_type}, {is_renew_code}, {is_renew}, {cur_package_paid_amount}, {cur_package_sales_amount}, {real_package_sales_amt}, {renew_package_sales_amount}, {package_level}, {action_names}, {renew_package_level}, {avg_applicants_num}, {in_invoice_avg_amt}, {out_invoice_avg_amt}, {is_compliance_tax_target}, {active_compliance_status},\n" +
                        "{lastest_follow_time}, {follow_type}, {follow_type_name}, {follow_progress}, {follow_progress_name}, {follow_intention}, {follow_intention_name}, {lastest_payment_method}, {lastest_payment_method_name}, {lastest_payment_time}, {lastest_order_time}, {renew_type}, {renew_type_code}, {cust_mgr_id}, {cust_mgr_name}, {province_city_area_code}, {big_region_code}, {is_order})");

        // 例5: cs_board_auth_monthly 模板
        templates.put("cs_board_auth_monthly",
                "INSERT INTO cctest_not_delete.ads_cs_board_authorization_monthly " +
                        "(true_id, `month`, agent_id, agent_name, sale_area_id, agent_user_code, num) " +
                        "VALUES " +
                        "({true_id}, {month}, {agent_id}, {agent_name}, {sale_area_id}, {agent_user_code}, {num})");

        // 例6: customer_review_expire_summary 汇总模板
        templates.put("customer_review_expire_summary_delete",
                "DELETE FROM cctest_not_delete.ads_itcrm_customer_review_zx_product_expire_summary WHERE end_date_month = '{end_date_month}' AND big_region_code = '{big_region_code}'");

        templates.put("customer_review_expire_summary_insert",
                "INSERT INTO cctest_not_delete.ads_itcrm_customer_review_zx_product_expire_summary\n" +
                        "SELECT t.end_date_month as end_date_month,\n" +
                        "     t.province_city_area_code as province_city_area_code,\n" +
                        "      t.outlet_code as outlet_code\n" +
                        "      ,t.cust_mgr_id as cust_mgr_id\n" +
                        "      ,t.cust_mgr_name\n" +
                        "      ,count( distinct t.customer_id) as customer_cnt\n" +
                        "      ,count( case when t.is_renew_code='Y' then t.customer_id end) as is_renew_cnt\n" +
                        "      ,count( case when t.renew_type_code='unRenewed' then t.customer_id end) as not_renew_cnt\n" +
                        "      ,sum(case when t.is_renew_code='Y' then t.real_package_sales_amt end) as real_package_sales_total_amt  -- 已续费产品包实际价值\n" +
                        "      ,sum(t.cur_package_paid_amount) as cur_package_paid_total_amt  -- 当前月份到期户产品包实收金额\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='upgrade' then t.customer_id end) as upgrade_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='upgrade' and t.renew_type_code='upgrade' then t.customer_id end) as upgrade_upgrade_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='upgrade' and t.renew_type_code='continuation' then t.customer_id end) as upgrade_continuation_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='upgrade' and t.renew_type_code='reduced' then t.customer_id end) as upgrade_reduced_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='upgrade' and t.renew_type_code='unRenewed' then t.customer_id end) as upgrade_unRenewed_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='continuation' then t.customer_id end) as continuation_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='continuation' and t.renew_type_code='upgrade' then t.customer_id end) as continuation_upgrade_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='continuation' and t.renew_type_code='continuation' then t.customer_id end) as continuation_continuation_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='continuation' and t.renew_type_code='reduced' then t.customer_id end) as continuation_reduced_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='continuation' and t.renew_type_code='unRenewed' then t.customer_id end) as continuation_unRenewed_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='earlyRenewal' then t.customer_id end) as earlyRenewal_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='earlyRenewal' and t.renew_type_code='upgrade' then t.customer_id end) as earlyRenewal_upgrade_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='earlyRenewal' and t.renew_type_code='continuation' then t.customer_id end) as earlyRenewal_continuation_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='earlyRenewal' and t.renew_type_code='reduced' then t.customer_id end) as earlyRenewal_reduced_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='earlyRenewal' and t.renew_type_code='unRenewed' then t.customer_id end) as earlyRenewal_unRenewed_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='lose' then t.customer_id end) as lose_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='lose' and t.renew_type_code='upgrade' then t.customer_id end) as lose_upgrade_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='lose' and t.renew_type_code='continuation' then t.customer_id end) as lose_continuation_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='lose' and t.renew_type_code='reduced' then t.customer_id end) as lose_reduced_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='lose' and t.renew_type_code='unRenewed' then t.customer_id end) as lose_unRenewed_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='reduced' then t.customer_id end) as reduced_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='reduced' and t.renew_type_code='upgrade' then t.customer_id end) as reduced_upgrade_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='reduced' and t.renew_type_code='continuation' then t.customer_id end) as reduced_continuation_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='reduced' and t.renew_type_code='reduced' then t.customer_id end) as reduced_reduced_cnt\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='reduced' and t.renew_type_code='unRenewed' then t.customer_id end) as reduced_unRenewed_cnt\n" +
                        "      ,case when t.big_region_code is not null then t.big_region_code else '000000' end as big_region_code\n" +
                        "      ,count(case when t.renew_type_code='unRenewed' and (t.is_order='N') then t.customer_id end) as unRenewed_unorder_cnt  -- 未续费未下单\n" +
                        "      ,count(case when t.renew_type_code='unRenewed' and (t.is_order='Y') then t.customer_id end) as unRenewed_order_cnt  -- 未续费已下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='upgrade' and t.renew_type_code='unRenewed' and t.is_order='N' then t.customer_id end) as upgrade_unRenewed_unOrder_cnt -- 升版未续费未下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='upgrade' and t.renew_type_code='unRenewed' and t.is_order='Y' then t.customer_id end) as upgrade_unRenewed_order_cnt -- 升版未续费已下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='continuation' and t.renew_type_code='unRenewed' and t.is_order='N' then t.customer_id end) as continuation_unRenewed_unOrder_cnt  -- 平续未续费未下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='continuation' and t.renew_type_code='unRenewed' and  t.is_order='Y' then t.customer_id end) as continuation_unRenewed_order_cnt  -- 平续未续费已下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='earlyRenewal' and t.renew_type_code='unRenewed' and t.is_order='N' then t.customer_id end) as earlyRenewal_unRenewed_unOrder_cnt  -- 提前续未续费未下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='earlyRenewal' and t.renew_type_code='unRenewed' and t.is_order='Y' then t.customer_id end) as earlyRenewal_unRenewed_order_cnt -- 提前续未续费已下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='lose' and t.renew_type_code='unRenewed' and t.is_order='N' then t.customer_id end) as lose_unRenewed_unOrder_cnt  -- 流失未续费未下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='lose' and t.renew_type_code='unRenewed' and t.is_order='Y' then t.customer_id end) as lose_unRenewed_order_cnt  -- 流失未续费已下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='reduced' and t.renew_type_code='unRenewed' and t.is_order='N' then t.customer_id end) as reduced_unRenewed_unOrder_cnt  -- 降版未续费未下单\n" +
                        "      ,count( distinct case when t.expect_renew_type_code='reduced' and t.renew_type_code='unRenewed' and t.is_order='Y' then t.customer_id end) as reduced_unRenewed_order_cnt -- 降版未续费已下单\n" +
                        " FROM\n" +
                        "     cctest_not_delete.ads_itcrm_customer_review_zx_product_expire t\n" +
                        " WHERE t.cust_mgr_id != '000' and end_date_month='{end_date_month}' and big_region_code='{big_region_code}'\n" +
                        " GROUP BY\n" +
                        "     t.outlet_code\n" +
                        "        ,t.cust_mgr_id\n" +
                        "        ,t.cust_mgr_name\n" +
                        "        ,t.end_date_month\n" +
                        "        ,case when t.big_region_code is not null then t.big_region_code else '000000' end\n" +
                        "        ,t.province_city_area_code;");
    }

    public String getTemplate(String key) {
        return templates.get(key);
    }
}
