## **实现方案**

为满足在 ClickHouse 数据库中生成 `ads_consume_pts` 相关表数据的需求，我将按照以下步骤进行开发：

### **1. 基础架构调整**
- **Maven 配置**：在 [pom.xml](file:///d:/Projects/smartRuleDataGen/pom.xml) 中添加 ClickHouse JDBC 驱动依赖 (`com.clickhouse:clickhouse-jdbc`)。
- **环境配置**：在 [application.yml](file:///d:/Projects/smartRuleDataGen/src/main/resources/application.yml) 中配置 ClickHouse 数据库连接信息（地址：`10.199.141.62:8123`）。
- **数据源注册**：修改 [DataSourceConfig.java](file:///d:/Projects/smartRuleDataGen/src/main/java/com/smartdata/smartruledatagen/config/DataSourceConfig.java)，注册 `ckDataSource` 和 `ckJdbcTemplate`。
- **执行器扩展**：修改 [JdbcExecutor.java](file:///d:/Projects/smartRuleDataGen/src/main/java/com/smartdata/smartruledatagen/service/database/JdbcExecutor.java)，注入 `ckJdbcTemplate` 并支持 `ck` 类型的数据库执行。

### **2. 数据模型开发**
- **新增 DTO**：创建 `ConsumePtsRequest.java`，包含 `customer_id`、`customer_name`、`companyNum`、`deptNum`、`employeeNum`、`statistics_month`、`executeInsert` 等字段。
- **聚合实体类**：在内部定义用于存储和计算聚合数据的 POJO 类。

### **3. 核心生成逻辑实现**
在 [DataGenController.java](file:///d:/Projects/smartRuleDataGen/src/main/java/com/smartdata/smartruledatagen/controller/DataGenController.java) 中新增接口 `/api/datagen/consume-pts`，实现以下逻辑：
- **层级生成**：
    - 根据 `companyNum` 随机生成企业信息（ID、名称）。
    - 每个企业下根据 `deptNum` 随机生成部门信息。
    - 每个部门下根据 `employeeNum` 随机生成员工信息。
- **随机数据填充**：
    - 为员工层级生成随机的流量总数、报送人数、算税人数等度量指标。
    - 随机生成 `statistics_month` 范围内的 `total_last_update`、`create_time` 等时间戳。
- **数据聚合计算**：
    - **部门汇总**：按 `customer_id + company_id + dept_id` 对员工数据进行求和聚合。
    - **企业汇总**：按 `customer_id + company_id` 对员工数据进行求和聚合。
- **SQL 构建与执行**：
    - 使用字符串模板构建三张表的 `INSERT` 语句。
    - 如果 `executeInsert` 为 `true`，则通过 `ckJdbcTemplate` 执行 SQL 插入。

### **4. 验证与交付**
- 提供 API 接口文档及示例请求体。
- 验证生成的 SQL 语句语法符合 ClickHouse 标准。
- 验证聚合数据的逻辑准确性（求和结果正确）。
