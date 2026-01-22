package com.smartdata.smartruledatagen.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SqlTemplateRepository {

    private final Map<String, String> templates = new HashMap<>();

    public SqlTemplateRepository() {
        // 例1: ads_trade_kpi_zx_order_payment_cust_rt 模板
        templates.put("trade_kpi_payment",
                "INSERT INTO servyou_ads.ads_trade_kpi_zx_order_payment_cust_rt\n" +
                        "(payment_date, outlet_code, outlet_name, income_type, trade_type, cust_mgr_id, cust_mgr_name, cust_mgr_outlet_code, cust_mgr_outlet_name, income_khjl_id, income_khjl_name, income_khjl_outlet_code, income_khjl_outlet_name, zx_payment_amount, zx_group_payment_amount, group_id, group_name, zx_assessment_payment_amount, cust_mgr_big_region_code, cust_mgr_big_region_name, income_khjl_big_region_code, income_khjl_big_region_name, customer_id, customer_name)\n" +
                        "VALUES\n" +
                        "(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)");

        // 例3: ads_trade_kpi_zx_order_payment_detail_rt 模板 (Generator 3)
        templates.put("trade_kpi_payment_detail",
                "INSERT INTO servyou_ads.ads_trade_kpi_zx_order_payment_detail_rt\n" +
                        "(payment_date, customer_id, customer_name, zx_payment_amount)\n" +
                        "VALUES\n" +
                        "(%s, %s, %s, %s)");

        // 例4: ads_trade_kpi_cs_order_payment_rt 模板 (Generator 4)
        templates.put("cs_order_payment",
                "INSERT INTO cctest_not_delete.ads_trade_kpi_cs_order_payment_rt\n" +
                        "(payment_date, cust_mgr_id, cust_mgr_name, cust_mgr_outlet_code, cust_mgr_outlet_name, cust_mgr_big_region_code, cust_mgr_big_region_name, order_source, kpi_order_source, customer_sort, contract_income_id, join_order_detail_id, order_id, order_detail_id, order_item_id, product_customer_new_type, product_name, payment_amount, order_signer, order_signer_trueid, order_creator, order_creator_trueid, main_customer_id, main_customer_name, product_customer_id,product_customer_name, business_class, business_class_code, order_type, order_type_code)\n" +
                        "VALUES\n" +
                        "(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)");

        // 例2: ads_itcrm_customer_review_zx_product_expire 模板
        templates.put("customer_review_expire",
                "INSERT INTO cctest_not_delete.ads_itcrm_customer_review_zx_product_expire\n" +
                        "(end_date_month, end_date, customer_id, customer_name, outlet_code, expect_renew_type_code, expect_renew_type, is_renew_code, is_renew, cur_package_paid_amount, real_package_sales_amt, renew_package_sales_amount, package_level,action_names, renew_package_level, avg_applicants_num, in_invoice_avg_amt, out_invoice_avg_amt, is_compliance_tax_target, active_compliance_status,\n" +
                        "lastest_follow_time, follow_type, follow_type_name, follow_progress, follow_progress_name, follow_intention, follow_intention_name, lastest_payment_method_name, lastest_payment_time, lastest_order_time, renew_type, renew_type_code, cust_mgr_id, cust_mgr_name, province_city_area_code, big_region_code,is_order)\n" +
                        "VALUES\n" +
                        "(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)");
    }

    public String getTemplate(String key) {
        return templates.get(key);
    }
}
