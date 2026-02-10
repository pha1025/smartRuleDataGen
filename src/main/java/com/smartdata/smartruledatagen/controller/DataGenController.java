package com.smartdata.smartruledatagen.controller;

import com.smartdata.smartruledatagen.config.GeneratorConfigLoader;
import com.smartdata.smartruledatagen.dto.ConsumePtsRequest;
import com.smartdata.smartruledatagen.dto.DataGenRequest;
import com.smartdata.smartruledatagen.dto.DataGenResponse;
import com.smartdata.smartruledatagen.generator.GenericDataGenerator;
import com.smartdata.smartruledatagen.model.CustomerData;
import com.smartdata.smartruledatagen.model.rules.FieldRule;
import com.smartdata.smartruledatagen.model.rules.GeneratorDefinition;
import com.smartdata.smartruledatagen.model.rules.ReferenceDataFieldRule;
import com.smartdata.smartruledatagen.service.ReferenceDataManager;
import com.smartdata.smartruledatagen.service.SqlTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.smartdata.smartruledatagen.model.CustMgrData;
import com.smartdata.smartruledatagen.model.PersonInfoData;

@RestController
@RequestMapping("/api/datagen")
@RequiredArgsConstructor
@Slf4j
public class DataGenController {

    private final GeneratorConfigLoader generatorConfigLoader;
    private final GenericDataGenerator genericDataGenerator;
    private final ReferenceDataManager referenceDataManager;
    private final SqlTemplateRepository sqlTemplateRepository;
    private final Map<String, JdbcTemplate> jdbcTemplates;
    private final Random random = new Random();

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @PostMapping("/consume-pts")
    public DataGenResponse generateConsumePtsData(@RequestBody ConsumePtsRequest request) {
        log.info("generateConsumePtsData request: {}", request);
        DataGenResponse response = new DataGenResponse();
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplates.get("ckJdbcTemplate");
            if (request.isExecuteInsert() && jdbcTemplate == null) {
                response.setSuccess(false);
                response.setMessage("未找到 ClickHouse 数据源配置 (ckJdbcTemplate)");
                return response;
            }

            String statsMonthStr = request.getStatistics_month();
            YearMonth yearMonth = YearMonth.parse(statsMonthStr);
            DateTimeFormatter fullTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            List<EmployeeRecord> employeeRecords = new ArrayList<>();
            Random random = new Random();

            // 1. 生成员工明细数据
            for (int i = 0; i < request.getCompanyNum(); i++) {
                long companyId = Math.abs(ThreadLocalRandom.current().nextLong(1000000000000L, 9999999999999L));
                String companyName = "企业名称" + companyId;

                for (int j = 0; j < request.getDeptNum(); j++) {
                    long deptId = Math.abs(ThreadLocalRandom.current().nextLong(1000000000L, 9999999999L));
                    String deptName = "部门" + deptId;

                    for (int k = 0; k < request.getEmployeeNum(); k++) {
                        EmployeeRecord record = new EmployeeRecord();
                        record.customerId = request.getCustomer_id();
                        record.customerName = request.getCustomer_name();
                        record.companyId = companyId;
                        record.companyName = companyName;
                        record.deptId = deptId;
                        record.deptName = deptName;
                        record.employeeId = Math.abs(ThreadLocalRandom.current().nextLong(100000000000L, 999999999999L));
                        record.employeeName = "员工" + record.employeeId;
                        record.licenseType = "201";
                        record.licenseNumber = UUID.randomUUID().toString().substring(0, 16); // 模拟加密串
                        
                        record.totalCount = random.nextInt(10);
                        record.employeeDeclareCount = random.nextInt(5);
                        record.specialDeductionDeclareCount = random.nextInt(5);
                        record.taxCalculateCount = random.nextInt(5);
                        record.taxReportCount = random.nextInt(5);

                        // 随机生成月份内的日期和时间
                        int day = random.nextInt(yearMonth.lengthOfMonth()) + 1;
                        int hour = random.nextInt(24);
                        int min = random.nextInt(60);
                        int sec = random.nextInt(60);
                        LocalDateTime randomDateTime = yearMonth.atDay(day).atTime(hour, min, sec);
                        String timeStr = randomDateTime.format(fullTimeFormatter);
                        
                        record.totalLastUpdate = timeStr;
                        record.employeeDeclareLastUpdate = timeStr;
                        record.specialDeductionDeclareLastUpdate = timeStr;
                        record.taxCalculateLastUpdate = timeStr;
                        record.taxReportLastUpdate = timeStr;
                        
                        record.statisticsMonth = statsMonthStr;
                        record.eventMonth = statsMonthStr;
                        record.createTime = yearMonth.atDay(day).format(dayFormatter);

                        employeeRecords.add(record);
                    }
                }
            }

            List<String> allSqls = new ArrayList<>();

            // 2. 生成明细表 SQL
            String employeeSqlTemplate = "INSERT INTO `default`.ads_consume_pts_original_dept_employee_month_group_mi_final " +
                    "(customer_id, customer_name, company_id, company_name, dept_id, dept_name, employee_id, employee_name, license_type, license_number, total_count, total_last_update, employee_declare_count, employee_declare_last_update, special_deduction_declare_count, special_deduction_declare_last_update, tax_calculate_count, tax_calculate_last_update, tax_report_count, tax_report_last_update, statistics_month, create_time, event_month) " +
                    "VALUES (%d, '%s', %d, '%s', %d, '%s', %d, '%s', '%s', '%s', %d, '%s', %d, '%s', %d, '%s', %d, '%s', %d, '%s', '%s', '%s', '%s')";

            for (EmployeeRecord r : employeeRecords) {
                allSqls.add(String.format(employeeSqlTemplate,
                        r.customerId, r.customerName, r.companyId, r.companyName, r.deptId, r.deptName, r.employeeId, r.employeeName,
                        r.licenseType, r.licenseNumber, r.totalCount, r.totalLastUpdate, r.employeeDeclareCount, r.employeeDeclareLastUpdate,
                        r.specialDeductionDeclareCount, r.specialDeductionDeclareLastUpdate, r.taxCalculateCount, r.taxCalculateLastUpdate,
                        r.taxReportCount, r.taxReportLastUpdate, r.statisticsMonth, r.createTime, r.eventMonth));
            }

            // 3. 聚合生成部门汇总数据 SQL
            Map<String, AggregatedData> deptAggregates = new HashMap<>();
            for (EmployeeRecord r : employeeRecords) {
                String key = r.companyId + "_" + r.deptId;
                AggregatedData agg = deptAggregates.computeIfAbsent(key, k -> {
                    AggregatedData d = new AggregatedData();
                    d.customerId = r.customerId;
                    d.customerName = r.customerName;
                    d.companyId = r.companyId;
                    d.companyName = r.companyName;
                    d.deptId = r.deptId;
                    d.deptName = r.deptName;
                    d.statisticsMonth = r.statisticsMonth;
                    d.createTime = r.createTime;
                    d.eventMonth = r.eventMonth;
                    return d;
                });
                agg.totalCount += r.totalCount;
                agg.employeeDeclareCount += r.employeeDeclareCount;
                agg.specialDeductionDeclareCount += r.specialDeductionDeclareCount;
                agg.taxCalculateCount += r.taxCalculateCount;
                agg.taxReportCount += r.taxReportCount;
            }

            String deptSqlTemplate = "INSERT INTO `default`.ads_consume_pts_original_dept_month_group_mi_final " +
                    "(customer_id, customer_name, company_id, company_name, dept_id, dept_name, total_count, employee_declare_count, special_deduction_declare_count, tax_calculate_count, tax_report_count, statistics_month, create_time, event_month) " +
                    "VALUES (%d, '%s', %d, '%s', %d, '%s', %d, %d, %d, %d, %d, '%s', '%s', '%s')";

            for (AggregatedData r : deptAggregates.values()) {
                allSqls.add(String.format(deptSqlTemplate,
                        r.customerId, r.customerName, r.companyId, r.companyName, r.deptId, r.deptName,
                        r.totalCount, r.employeeDeclareCount, r.specialDeductionDeclareCount, r.taxCalculateCount, r.taxReportCount,
                        r.statisticsMonth, r.createTime, r.eventMonth));
            }

            // 4. 聚合生成企业汇总数据 SQL
            Map<Long, AggregatedData> companyAggregates = new HashMap<>();
            for (EmployeeRecord r : employeeRecords) {
                AggregatedData agg = companyAggregates.computeIfAbsent(r.companyId, k -> {
                    AggregatedData d = new AggregatedData();
                    d.customerId = r.customerId;
                    d.customerName = r.customerName;
                    d.companyId = r.companyId;
                    d.companyName = r.companyName;
                    d.statisticsMonth = r.statisticsMonth;
                    d.createTime = r.createTime;
                    d.eventMonth = r.eventMonth;
                    return d;
                });
                agg.totalCount += r.totalCount;
                agg.employeeDeclareCount += r.employeeDeclareCount;
                agg.specialDeductionDeclareCount += r.specialDeductionDeclareCount;
                agg.taxCalculateCount += r.taxCalculateCount;
                agg.taxReportCount += r.taxReportCount;
            }

            String companySqlTemplate = "INSERT INTO `default`.ads_consume_pts_original_company_month_group_mi_final " +
                    "(customer_id, customer_name, company_id, company_name, total_count, employee_declare_count, special_deduction_declare_count, tax_calculate_count, tax_report_count, statistics_month, create_time, event_month) " +
                    "VALUES (%d, '%s', %d, '%s', %d, %d, %d, %d, %d, '%s', '%s', '%s')";

            for (AggregatedData r : companyAggregates.values()) {
                allSqls.add(String.format(companySqlTemplate,
                        r.customerId, r.customerName, r.companyId, r.companyName,
                        r.totalCount, r.employeeDeclareCount, r.specialDeductionDeclareCount, r.taxCalculateCount, r.taxReportCount,
                        r.statisticsMonth, r.createTime, r.eventMonth));
            }

            // 5. 执行入库
            int successCount = 0;
            if (request.isExecuteInsert()) {
                for (String sql : allSqls) {
                    try {
                        jdbcTemplate.execute(sql);
                        successCount++;
                    } catch (Exception e) {
                        log.error("ClickHouse SQL execution failed: {}", sql, e);
                    }
                }
            }

            response.setSuccess(true);
            response.setMessage("ClickHouse 数据生成成功");
            response.setGeneratedCount(allSqls.size());
            response.setSuccessInsertCount(successCount);
            response.setSqls(allSqls);

        } catch (Exception e) {
            log.error("ClickHouse data generation failed", e);
            response.setSuccess(false);
            response.setMessage("生成失败: " + e.getMessage());
        }
        return response;
    }

    private DataGenResponse handleWorkloadStatisticsGeneration(DataGenRequest request, GeneratorDefinition definition, JdbcTemplate jdbcTemplate) {
        DataGenResponse response = new DataGenResponse();
        List<String> allSqls = new ArrayList<>();
        int successCount = 0;

        try {
            // 1. 确定日期 (cal_date)
            String calDateStr;
            Map<String, Object> extraParams = request.getExtraParams() != null ? request.getExtraParams() : new HashMap<>();
            String endDateMonth = (String) extraParams.get("endDateMonth");
            
            LocalDate today = LocalDate.now();
            if (endDateMonth != null && !endDateMonth.isEmpty() && !"null".equalsIgnoreCase(endDateMonth)) {
                YearMonth ym = YearMonth.parse(endDateMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
                if (ym.equals(YearMonth.from(today))) {
                     // 本月：随机一天且不超过当天
                     int maxDay = today.getDayOfMonth();
                     int randomDay = random.nextInt(maxDay) + 1;
                     calDateStr = today.withDayOfMonth(randomDay).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else {
                     // 历史月份：任意一天
                     int maxDay = ym.lengthOfMonth();
                     int randomDay = random.nextInt(maxDay) + 1;
                     calDateStr = ym.atDay(randomDay).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
            } else {
                 // 默认本月不超过当天
                 int maxDay = today.getDayOfMonth();
                 int randomDay = random.nextInt(maxDay) + 1;
                 calDateStr = today.withDayOfMonth(randomDay).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }

            // 2. 确定客户经理 (cust_mgr)
            List<CustMgrData> candidates;
            String regionCode = request.getRegionCode();
            String customerManageId = (String) extraParams.get("customerManageId");

            if (customerManageId != null && !customerManageId.trim().isEmpty() && !"null".equalsIgnoreCase(customerManageId)) {
                // 优先使用 customerManageId
                Optional<CustMgrData> mgrOpt = referenceDataManager.getCustMgrById(customerManageId.trim());
                if (mgrOpt.isPresent()) {
                    candidates = Collections.singletonList(mgrOpt.get());
                    log.info("Using specific manager from customerManageId: {}", customerManageId);
                } else {
                    log.warn("customerManageId {} not found, falling back to regionCode logic", customerManageId);
                    if (referenceDataManager.hasRegionData(regionCode)) {
                        candidates = referenceDataManager.getCustMgrsByBigRegion(regionCode);
                    } else {
                        response.setSuccess(false);
                        response.setMessage("无效的 regionCode: " + regionCode + " 且 customerManageId 未找到: " + customerManageId);
                        return response;
                    }
                }
            } else if (referenceDataManager.hasRegionData(regionCode)) {
                candidates = referenceDataManager.getCustMgrsByBigRegion(regionCode);
            } else {
                response.setSuccess(false);
                response.setMessage("无效的 regionCode: " + regionCode);
                return response;
            }

            // 3. 循环生成
            int count = request.getCount();
            for (int i = 0; i < count; i++) {
                CustMgrData mgr = candidates.get(random.nextInt(candidates.size()));
                
                // 3.1 生成主表数据
                int callNum = random.nextInt(11); // 0-10
                int thruNum = callNum > 0 ? random.nextInt(callNum + 1) : 0;
                int thru16sNum = thruNum > 0 ? random.nextInt(thruNum + 1) : 0;
                int thru30sNum = thru16sNum > 0 ? random.nextInt(thru16sNum + 1) : 0;
                int thru60sNum = thru30sNum > 0 ? random.nextInt(thru30sNum + 1) : 0;
                
                double chatDuration = Math.round(random.nextDouble() * 10000.0) / 100.0; // 随机两位小数
                int visitNum = random.nextInt(11);
                int remoteNum = random.nextInt(11);
                int wxworkNum = random.nextInt(11);
                int callFollowNum = random.nextInt(11);
                int wxworkFollowNum = random.nextInt(11);

                String mainSqlTemplate = sqlTemplateRepository.getTemplate("itcrm_cust_mgr_workload_statistics");
                String mainSql = mainSqlTemplate
                        .replace("{cal_date}", "'" + calDateStr + "'")
                        .replace("{cust_mgr_id}", "'" + mgr.getCustMgrId() + "'")
                        .replace("{cust_mgr_outlet_code}", "'" + mgr.getCustMgrOutletCode() + "'")
                        .replace("{cust_mgr_name}", "'" + mgr.getCustMgrName() + "'")
                        .replace("{cust_mgr_outlet_name}", "'" + mgr.getCustMgrOutletName() + "'")
                        .replace("{call_num}", String.valueOf(callNum))
                        .replace("{thru_num}", String.valueOf(thruNum))
                        .replace("{thru_16s_num}", String.valueOf(thru16sNum))
                        .replace("{thru_30s_num}", String.valueOf(thru30sNum))
                        .replace("{thru_60s_num}", String.valueOf(thru60sNum))
                        .replace("{chat_duration}", String.valueOf(chatDuration))
                        .replace("{visit_num}", String.valueOf(visitNum))
                        .replace("{remote_num}", String.valueOf(remoteNum))
                        .replace("{wxwork_num}", String.valueOf(wxworkNum))
                        .replace("{call_follow_num}", String.valueOf(callFollowNum))
                        .replace("{wxwork_follow_num}", String.valueOf(wxworkFollowNum));
                allSqls.add(mainSql);

                // 3.2 生成子表1 (通话明细)
                // 逻辑：thru60s -> 产生5条 (call, thru, thru_16s, thru_30s, thru_60s)
                //      thru30s-thru60s -> 产生4条
                //      thru16s-thru30s -> 产生3条
                //      thru-thru16s -> 产生2条 (call, thru)
                //      call-thru -> 产生1条 (call, status=fail)

                String detail1Template = sqlTemplateRepository.getTemplate("itcrm_cust_mgr_workload_detail");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                
                // 处理接通的 (递归逻辑)
                // 我们可以倒序处理，从 thru60s 开始
                // 实际生成的记录数 = thru60s * 5 + (thru30s-thru60s)*4 + ...
                
                // 简化生成逻辑：定义每种最终状态需要的 flag 集合
                // Thru 60s: [call, thru, thru_16s, thru_30s, thru_60s]
                // Thru 30s: [call, thru, thru_16s, thru_30s]
                // Thru 16s: [call, thru, thru_16s]
                // Thru:     [call, thru]
                // Failed:   [call]

                int count60s = thru60sNum;
                int count30s = thru30sNum - thru60sNum;
                int count16s = thru16sNum - thru30sNum;
                int countThru = thruNum - thru16sNum;
                int countFail = callNum - thruNum;

                // Helper to generate a batch of records for one call event
                generateCallDetails(allSqls, detail1Template, calDateStr, mgr, count60s, 60, timeFormatter);
                generateCallDetails(allSqls, detail1Template, calDateStr, mgr, count30s, 30, timeFormatter);
                generateCallDetails(allSqls, detail1Template, calDateStr, mgr, count16s, 16, timeFormatter);
                generateCallDetails(allSqls, detail1Template, calDateStr, mgr, countThru, 0, timeFormatter); // 0 means connected but < 16s
                generateCallDetails(allSqls, detail1Template, calDateStr, mgr, countFail, -1, timeFormatter); // -1 means failed

                // 3.3 生成子表2 (上门明细)
                String detail2Template = sqlTemplateRepository.getTemplate("itcrm_cust_mgr_workload_visit_door_detail");
                List<CustomerData> customers = referenceDataManager.getCustomersByManageId(mgr.getCustMgrId());
                // 如果该经理没客户，随机取该区域的
                if (customers.isEmpty()) {
                    customers = referenceDataManager.getCustomersByRegion(regionCode);
                }
                
                for (int v = 0; v < visitNum; v++) {
                     CustomerData cust = customers.isEmpty() ? null : customers.get(random.nextInt(customers.size()));
                     String companyId = cust != null ? cust.getCustomerId() : UUID.randomUUID().toString().replace("-", "");
                     String custName = cust != null ? cust.getCustomerName() : "未知客户";
                     
                     String randomTime = generateRandomTime(calDateStr, timeFormatter);

                     String sql = detail2Template
                             .replace("{cal_date}", "'" + calDateStr + "'")
                             .replace("{cust_mgr_id}", "'" + mgr.getCustMgrId() + "'")
                             .replace("{cust_mgr_outlet_code}", "'" + mgr.getCustMgrOutletCode() + "'")
                             .replace("{company_id}", "'" + companyId + "'")
                             .replace("{customer_name}", "'" + custName + "'")
                             .replace("{sign_in_time}", "'" + randomTime + "'")
                             .replace("{sign_in_address}", "'默认签到地址'")
                             .replace("{sign_out_time}", "'" + randomTime + "'")
                             .replace("{sign_out_address}", "'默认签退地址'")
                             .replace("{contact_time}", "'" + randomTime + "'")
                             .replace("{business_action}", "'默认拜访'")
                             .replace("{specific_item}", "'默认事项'")
                             .replace("{follow_result}", "'默认结果'")
                             .replace("{follow_record}", "'默认记录'")
                             .replace("{create_time}", "'" + randomTime + "'");
                     allSqls.add(sql);
                }

                // 3.4 生成子表3 (跟进记录明细)
                String detail3Template = sqlTemplateRepository.getTemplate("itcrm_cust_mgr_workload_follow_record_detail");
                
                generateFollowDetails(allSqls, detail3Template, calDateStr, mgr, remoteNum, "remote", customers, timeFormatter);
                generateFollowDetails(allSqls, detail3Template, calDateStr, mgr, wxworkFollowNum, "wxwork", customers, timeFormatter);
                generateFollowDetails(allSqls, detail3Template, calDateStr, mgr, callFollowNum, "call", customers, timeFormatter);
            }

            // 4. 执行
            if (request.isExecuteInsert()) {
                for (String sql : allSqls) {
                    try {
                        jdbcTemplate.execute(sql);
                        successCount++;
                    } catch (Exception e) {
                        log.error("Failed to execute SQL: {}", sql, e);
                    }
                }
            }

            response.setSuccess(true);
            response.setMessage("工作量统计数据生成成功");
            response.setGeneratedCount(allSqls.size());
            response.setSuccessInsertCount(successCount);
            response.setSqls(allSqls);

        } catch (Exception e) {
            log.error("Workload stats generation failed", e);
            response.setSuccess(false);
            response.setMessage("生成失败: " + e.getMessage());
        }
        return response;
    }

    private void generateCallDetails(List<String> sqls, String template, String calDate, CustMgrData mgr, int count, int type, DateTimeFormatter formatter) {
        // type: 60 (>=60s), 30 (30-60), 16 (16-30), 0 (0-16), -1 (fail)
        for (int i = 0; i < count; i++) {
            PersonInfoData person = referenceDataManager.getRandomPersonInfo();
            String phone = person != null ? maskPhone(person.getCalledPhoneNumber()) : "138****0000";
            String personId = person != null ? person.getPersonId() : "PID" + random.nextInt(100000);
            
            // Generate base time and duration
            String startTimeStr = generateRandomTime(calDate, formatter);
            int duration = 0;
            String status = "接通";
            String recordingAddr = "'https://servu-crm.oss-cn-hangzhou.aliyuncs.com/demo.wav'";
            String callRecordId = String.valueOf(Math.abs(random.nextLong()));
            
            if (type == -1) {
                status = "骚扰拦截";
                // duration, start_time, end_time is NULL for failed calls in some requirements, but user said:
                // "若call_flag-thru＞0... status=骚扰拦截...且chat_duration、start_time和end_time为null"
                // But template has placeholders. We need to handle NULLs in SQL string.
            } else {
                // Determine duration based on type
                if (type == 60) duration = 60 + random.nextInt(60);
                else if (type == 30) duration = 30 + random.nextInt(30);
                else if (type == 16) duration = 16 + random.nextInt(14);
                else duration = 1 + random.nextInt(15);
            }

            // Flags to generate
            List<String> flags = new ArrayList<>();
            flags.add("call");
            if (type >= 0) flags.add("thru");
            if (type >= 16) flags.add("thru_16s");
            if (type >= 30) flags.add("thru_30s");
            if (type >= 60) flags.add("thru_60s");

            for (String flag : flags) {
                String sql = template
                        .replace("{cal_date}", "'" + calDate + "'")
                        .replace("{cust_mgr_id}", "'" + mgr.getCustMgrId() + "'")
                        .replace("{cust_mgr_outlet_code}", "'" + mgr.getCustMgrOutletCode() + "'")
                        .replace("{call_flag}", "'" + flag + "'")
                        .replace("{cust_mgr_name}", "'" + mgr.getCustMgrName() + "'")
                        .replace("{cust_mgr_outlet_name}", "'" + mgr.getCustMgrOutletName() + "'")
                        .replace("{called_phone_number}", "'" + phone + "'")
                        .replace("{call_direction}", "'呼出'")
                        .replace("{call_record_id}", callRecordId)
                        .replace("{person_id}", "'" + personId + "'");
                
                if (type == -1 && "call".equals(flag)) {
                     // Special case for failed call
                     sql = sql.replace("{chat_duration}", "NULL")
                              .replace("{start_time}", "NULL")
                              .replace("{end_time}", "NULL")
                              .replace("{create_date}", "NULL") // User said create_date=start_time
                              .replace("{status}", "'" + status + "'")
                              .replace("{recording_address}", "NULL");
                } else {
                    LocalDateTime start = LocalDateTime.parse(startTimeStr, formatter);
                    LocalDateTime end = start.plusSeconds(duration);
                    sql = sql.replace("{chat_duration}", String.valueOf(duration))
                             .replace("{start_time}", "'" + startTimeStr + "'")
                             .replace("{end_time}", "'" + end.format(formatter) + "'")
                             .replace("{create_date}", "'" + startTimeStr + "'")
                             .replace("{status}", "'" + status + "'")
                             .replace("{recording_address}", recordingAddr);
                }
                sqls.add(sql);
            }
        }
    }

    private void generateFollowDetails(List<String> sqls, String template, String calDate, CustMgrData mgr, int count, String followFlag, List<CustomerData> customers, DateTimeFormatter formatter) {
        for (int i = 0; i < count; i++) {
            CustomerData cust = customers.isEmpty() ? null : customers.get(random.nextInt(customers.size()));
            String companyId = cust != null ? cust.getCustomerId() : UUID.randomUUID().toString().replace("-", "");
            String custName = cust != null ? cust.getCustomerName() : "未知客户";
            PersonInfoData person = referenceDataManager.getRandomPersonInfo();
            String personId = person != null ? person.getPersonId() : "PID" + random.nextInt(100000);
            String randomTime = generateRandomTime(calDate, formatter);

            String sql = template
                    .replace("{cal_date}", "'" + calDate + "'")
                    .replace("{cust_mgr_id}", "'" + mgr.getCustMgrId() + "'")
                    .replace("{cust_mgr_outlet_code}", "'" + mgr.getCustMgrOutletCode() + "'")
                    .replace("{company_id}", "'" + companyId + "'")
                    .replace("{customer_name}", "'" + custName + "'")
                    .replace("{person_id}", "'" + personId + "'")
                    .replace("{contact_time}", "'" + randomTime + "'")
                    .replace("{business_action}", "'默认跟进'")
                    .replace("{specific_item}", "'默认事项'")
                    .replace("{follow_result}", "'默认结果'")
                    .replace("{create_time}", "'" + randomTime + "'")
                    .replace("{follow_way}", "'默认方式'")
                    .replace("{follow_flag}", "'" + followFlag + "'");
            sqls.add(sql);
        }
    }

    private String generateRandomTime(String dateStr, DateTimeFormatter formatter) {
        // dateStr is yyyy-MM-dd
        LocalDate date = LocalDate.parse(dateStr);
        return date.atTime(random.nextInt(24), random.nextInt(60), random.nextInt(60)).format(formatter);
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private static class EmployeeRecord {
        long customerId;
        String customerName;
        long companyId;
        String companyName;
        long deptId;
        String deptName;
        long employeeId;
        String employeeName;
        String licenseType;
        String licenseNumber;
        int totalCount;
        String totalLastUpdate;
        int employeeDeclareCount;
        String employeeDeclareLastUpdate;
        int specialDeductionDeclareCount;
        String specialDeductionDeclareLastUpdate;
        int taxCalculateCount;
        String taxCalculateLastUpdate;
        int taxReportCount;
        String taxReportLastUpdate;
        String statisticsMonth;
        String createTime;
        String eventMonth;
    }

    private static class AggregatedData {
        long customerId;
        String customerName;
        long companyId;
        String companyName;
        long deptId;
        String deptName;
        long totalCount = 0;
        long employeeDeclareCount = 0;
        long specialDeductionDeclareCount = 0;
        long taxCalculateCount = 0;
        long taxReportCount = 0;
        String statisticsMonth;
        String createTime;
        String eventMonth;
    }

    @PostMapping("/generate")
    public DataGenResponse generateData(@RequestBody DataGenRequest request) {
        log.info("generateData request: {}", request);
        DataGenResponse response = new DataGenResponse();
        try {
            // 1. 验证生成器是否存在
            GeneratorDefinition definition = generatorConfigLoader.getRuleConfig().getGenerators().get(request.getGeneratorName());
            if (definition == null) {
                response.setSuccess(false);
                response.setMessage("生成器不存在: " + request.getGeneratorName());
                return response;
            }

            // 获取对应的 JdbcTemplate
            String dbKey = definition.getDbKey(); // e.g., "db1"
            JdbcTemplate jdbcTemplate = jdbcTemplates.get(dbKey + "JdbcTemplate");
            if (request.isExecuteInsert() && jdbcTemplate == null) {
                response.setSuccess(false);
                response.setMessage("未找到对应的数据源配置: " + dbKey);
                return response;
            }

            // 1.5 针对汇总类生成器的特殊处理
            if ("customerReviewExpireSummary".equals(request.getGeneratorName())) {
                return handleSummaryGeneration(request, definition, jdbcTemplate);
            }
            if ("itcrmCustMgrWorkloadStatistics".equals(request.getGeneratorName())) {
                return handleWorkloadStatisticsGeneration(request, definition, jdbcTemplate);
            }

            // 2. 获取客户数据 (支持通过 extraParams 指定经理或具体客户)
            List<CustomerData> candidates = new ArrayList<>();
            Map<String, Object> extraParams = request.getExtraParams() != null ? request.getExtraParams() : new HashMap<>();

            String customerManageId = (String) extraParams.get("customerManageId");

              if (customerManageId != null && !customerManageId.trim().isEmpty() && !"null".equalsIgnoreCase(customerManageId)) {
                  // 情况 A: 仅提供了经理 ID
                  candidates = referenceDataManager.getCustomersByManageId(customerManageId.trim());
                  log.info("Found {} customers for manager {}", candidates.size(), customerManageId);
              } else {
                 // 情况 B: 兜底使用 regionCode 逻辑
                if (request.getRegionCode() == null || request.getRegionCode().isEmpty()) {
                    response.setSuccess(false);
                    response.setMessage("地区代码不能为空 (当未指定 customerManageId 时)");
                    return response;
                }
                if (!referenceDataManager.hasRegionData(request.getRegionCode())) {
                    response.setSuccess(false);
                    response.setMessage("地区代码不存在或该地区无客户经理数据: " + request.getRegionCode());
                    return response;
                }
                candidates = referenceDataManager.getCustomersByRegion(request.getRegionCode());
                log.info("Total candidates for region {}: {}", request.getRegionCode(), candidates.size());
            }

            // 针对业务逻辑的进一步筛选
            if ("tradeKpiPayment".equals(request.getGeneratorName())) {
                candidates = candidates.stream()
                        .filter(c -> "1".equals(c.getCustomerType()))
                        .collect(Collectors.toList());
                log.info("Candidates after filtering type=1: {}", candidates.size());
            }
            // 针对 csBoardAuthMonthly 生成器，只筛选 type=2/3 的客户
            if ("csBoardAuthMonthly".equals(request.getGeneratorName())) {
                candidates = candidates.stream()
                        .filter(c -> "2".equals(c.getCustomerType()) || "3".equals(c.getCustomerType()))
                        .collect(Collectors.toList());
            }

            if (candidates.isEmpty()) {
                response.setSuccess(false);
                response.setMessage("该地区下未找到任何客户数据" + ("tradeKpiPayment".equals(request.getGeneratorName()) ? " (Type=1)" : ""));
                return response;
            }

            // 3. 确定生成数量和提示
            int requestCount = request.getCount();
            int actualCount = requestCount;
            String message = "生成成功";
            
            if (requestCount > candidates.size()) {
                actualCount = candidates.size();
                message = String.format("请求生成 %d 条，但该地区只有 %d 条可用客户数据。已生成 %d 条。", requestCount, candidates.size(), actualCount);
            }

            // 4. 准备预置数据 (乱序并截取)
            List<CustomerData> selectedCustomers = new ArrayList<>(candidates);
            Collections.shuffle(selectedCustomers);
            selectedCustomers = selectedCustomers.subList(0, actualCount);

            // 5. 映射字段名
            String idFieldName = "customer_id"; // 默认
            String nameFieldName = "customer_name"; // 默认

            // 尝试从生成器定义中智能查找字段名
            for (FieldRule rule : definition.getFields()) {
                if (rule instanceof ReferenceDataFieldRule refRule) {
                    if ("CustomerData".equalsIgnoreCase(refRule.getRefDataType())) {
                        if ("id".equalsIgnoreCase(refRule.getFieldToPopulate())) {
                            idFieldName = refRule.getName();
                        } else if ("name".equalsIgnoreCase(refRule.getFieldToPopulate())) {
                            nameFieldName = refRule.getName();
                        }
                    }
                }
            }
            
            // 特殊处理 csOrderPayment，因为它可能有多个 CustomerData 引用
            if ("csOrderPayment".equals(request.getGeneratorName())) {
                idFieldName = "main_customer_id";
                nameFieldName = "main_customer_name";
            }

            List<Map<String, Object>> preDefinedRecords = new ArrayList<>();
            for (CustomerData customer : selectedCustomers) {
                Map<String, Object> record = new HashMap<>();
                record.put(idFieldName, customer.getCustomerId());
                record.put(nameFieldName, customer.getCustomerName());
                
                // 将客户经理ID预置进去
                String finalManageId = (customerManageId != null && !customerManageId.trim().isEmpty() && !"null".equalsIgnoreCase(customerManageId))
                        ? customerManageId.trim()
                        : customer.getCustomerManageId();

                if (finalManageId != null) {
                    record.put("customer_manage_id", finalManageId);
                    // 同时也预置到生成器常用的经理 ID 字段中，这样会跳过 YAML 中的随机/引用规则
                    record.put("cust_mgr_id", finalManageId);
                    record.put("signer_id", finalManageId);
                    record.put("income_khjl_id", finalManageId);
                    record.put("income_khjl_id_detail", finalManageId);
                }
                if (customer.getServyouNum() != null) {
                    record.put("servyou_num", customer.getServyouNum());
                }
                if (customer.getSaleAreaId() != null) {
                    record.put("sale_area_id", customer.getSaleAreaId());
                }
                if (customer.getProvinceCityAreaCode() != null) {
                    record.put("province_city_area_code", customer.getProvinceCityAreaCode());
                }

                    preDefinedRecords.add(record);
            }

            // 6. 生成 SQL
            Map<String, Object> params = new HashMap<>();
            // 传递 regionCode 供规则使用 (e.g. lookupProvinceByRegion)
            params.put("regionCode", request.getRegionCode());
            params.put("bigRegionCode", request.getRegionCode()); // 兼容旧名称

            // 合并扩展参数
            if (request.getExtraParams() != null) {
                params.putAll(request.getExtraParams());
            }

            List<String> sqls = genericDataGenerator.generateSqls(definition, actualCount, params, preDefinedRecords);

            // 7. 执行插入 (如果需要)
            int successInsertCount = 0;
            if (request.isExecuteInsert()) {
                for (String sql : sqls) {
                    try {
                        // 移除可能的末尾分号
                        String cleanSql = sql.trim();
                        if (cleanSql.endsWith(";")) {
                            cleanSql = cleanSql.substring(0, cleanSql.length() - 1);
                        }
                        jdbcTemplate.execute(cleanSql);
                        successInsertCount++;
                    } catch (Exception e) {
                        log.error("Failed to execute SQL: {}", sql, e);
                        // 可以选择继续或中断，这里选择继续并统计成功数
                    }
                }
            }

            response.setSuccess(true);
            response.setMessage(message);
            // generatedCount 应该返回生成的“记录数”（主表数据量），即 actualCount
            // 而不是 sqls.size() (因为现在可能包含详情表等多条SQL)
            response.setGeneratedCount(actualCount);
            response.setSuccessInsertCount(successInsertCount);
            response.setSqls(sqls);

        } catch (Exception e) {
            log.error("Generation failed", e);
            response.setSuccess(false);
            response.setMessage("生成失败: " + e.getMessage());
        }
        return response;
    }

    private DataGenResponse handleSummaryGeneration(DataGenRequest request, GeneratorDefinition definition, JdbcTemplate jdbcTemplate) {
        DataGenResponse response = new DataGenResponse();
        Map<String, Object> extraParams = request.getExtraParams();

        String endDateMonth = null;
        if (extraParams != null && extraParams.containsKey("endDateMonth") && extraParams.get("endDateMonth") != null) {
            String val = String.valueOf(extraParams.get("endDateMonth")).trim();
            if (!val.isEmpty() && !"null".equalsIgnoreCase(val)) {
                endDateMonth = val;
            }
        }

        if (endDateMonth == null) {
            endDateMonth = YearMonth.now().toString(); // e.g. 2026-02
        }

        // 优先从 extraParams 获取 regionCode，如果没有则取外层的
        String bigRegionCode = (extraParams != null && extraParams.containsKey("regionCode"))
                ? String.valueOf(extraParams.get("regionCode"))
                : request.getRegionCode();

        if (bigRegionCode == null || bigRegionCode.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("参数 regionCode 不能为空");
            return response;
        }

        try {
            // 1. 执行删除操作 (如果配置了)
            if (definition.getExtraSqlTemplateKeys() != null && !definition.getExtraSqlTemplateKeys().isEmpty()) {
                for (String deleteKey : definition.getExtraSqlTemplateKeys()) {
                    String deleteSqlTemplate = sqlTemplateRepository.getTemplate(deleteKey);
                    if (deleteSqlTemplate != null) {
                        String deleteSql = deleteSqlTemplate
                                .replace("{end_date_month}", endDateMonth)
                                .replace("{big_region_code}", bigRegionCode);
                        log.info("Executing summary delete SQL: {}", deleteSql);
                        jdbcTemplate.execute(deleteSql);
                    }
                }
            }

            // 2. 执行汇总插入操作
            String insertSqlTemplate = sqlTemplateRepository.getTemplate(definition.getSqlTemplateKey());
            if (insertSqlTemplate == null) {
                throw new RuntimeException("汇总插入模板未找到: " + definition.getSqlTemplateKey());
            }

            String insertSql = insertSqlTemplate
                    .replace("{end_date_month}", endDateMonth)
                    .replace("{big_region_code}", bigRegionCode);

            log.info("Executing summary insert SQL: {}", insertSql);
            // 使用 update 以获取受影响的行数
            int rows = jdbcTemplate.update(insertSql);

            response.setSuccess(true);
            response.setMessage("汇总数据生成成功");
            response.setGeneratedCount(rows);
            response.setSuccessInsertCount(rows);
            response.setSqls(Collections.singletonList(insertSql));

        } catch (Exception e) {
            log.error("Summary generation failed", e);
            response.setSuccess(false);
            response.setMessage("汇总生成失败: " + e.getMessage());
        }
        return response;
    }
}
