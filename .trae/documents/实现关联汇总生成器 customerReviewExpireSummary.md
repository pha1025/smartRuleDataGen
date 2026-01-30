## **技术实现方案**

### **1. DTO 结构优化**
- 在 [DataGenRequest.java](file:///d:/Projects/smartRuleDataGen/src/main/java/com/smartdata/smartruledatagen/dto/DataGenRequest.java) 中新增 `Map<String, Object> extraParams` 字段，用于承载 `endDateMonth`、`regionCode` 及未来可能的 `customerManageId`。

### **2. SQL 模板配置**
- 在 [SqlTemplateRepository.java](file:///d:/Projects/smartRuleDataGen/src/main/java/com/smartdata/smartruledatagen/service/SqlTemplateRepository.java) 中注册以下模板：
    - `customer_review_expire_summary_delete`: 使用占位符 `{end_date_month}` 和 `{big_region_code}`。
    - `customer_review_expire_summary_insert`: 使用占位符封装您提供的 `INSERT INTO ... SELECT ...` 复杂 SQL。

### **3. 生成器规则定义**
- 在 [generator-rules.yml](file:///d:/Projects/smartRuleDataGen/src/main/resources/generator-rules.yml) 中新增 `customerReviewExpireSummary` 定义，明确其使用 `db2` 数据源。

### **4. 控制器逻辑分支**
- 在 [DataGenController.java](file:///d:/Projects/smartRuleDataGen/src/main/java/com/smartdata/smartruledatagen/controller/DataGenController.java) 中插入特殊处理分支：
    - **参数校验**：从 `extraParams` 提取 `endDateMonth`。若缺失，立即返回 `500` 或自定义错误信息。
    - **跳过校验**：不执行针对 Excel 文件的 `regionCode` 存在性校验。
    - **SQL 执行**：
        1. 获取 `db2` 的 `JdbcTemplate`。
        2. 格式化并执行 `DELETE` SQL。
        3. 格式化并执行 `INSERT ... SELECT` SQL。
    - **响应封装**：返回操作成功提示及受影响的记录数。

## **验证步骤**
1. 发送不带 `endDateMonth` 的请求，验证报错提醒。
2. 发送完整参数请求，观察日志中 SQL 的替换是否正确（尤其是大区代码和月份）。
3. 检查数据库 `ads_itcrm_customer_review_zx_product_expire_summary` 表的结果。
