# 修改后的生成器实施计划

根据您的最新诉求，我们对计划进行了细化和调整：

## 1. 代码层面修改

### 1.1 数据模型扩展
*   **`CustomerData.java`**: 新增 `customerManageId` 字段，用于存储关联的客户经理 ID。
*   **`ExcelDataLoaderService.java`**: 修改 Excel 解析逻辑，读取新增的第 4 列（假设为 `customer_manage_id`）。

### 1.2 生成逻辑增强 (`GenericDataGenerator.java` & `ExpressionEvaluator.java`)
*   **支持多字段获取**: 扩展 `CustomerData` 的引用逻辑，支持通过 `main_customer_id` 反查 `customerManageId`。
*   **新增日期函数**: 在表达式计算器中新增 `randomDateInCurrentMonth()` 函数，实现“当月日期且不超过当前日期”的逻辑。
*   **枚举支持**: 确保枚举查找逻辑适配新的业务类型字段。

## 2. 配置文件规则 (`generator-rules.yml`)

新增 `csOrderPayment` 生成器，关键字段逻辑如下：

*   **辅助字段** (不输出到 SQL，仅用于逻辑控制):
    *   `_temp_cust_type`: `LIST_RANDOM` -> `[2, 3]` (随机选择类型 2 或 3)。

*   **客户与关联客户经理**:
    *   `main_customer_id`: `REFERENCE_DATA` -> 根据 `_temp_cust_type` 筛选客户。
    *   `main_customer_name`: `REFERENCE_DATA` -> 根据 `main_customer_id` 获取。
    *   `cust_mgr_id`: `REFERENCE_DATA` -> 根据 `main_customer_id` 获取关联的 `manageId` (**核心关联逻辑**)。
    *   `cust_mgr_name` 等: `REFERENCE_DATA` -> 根据 `cust_mgr_id` (CustMgrData) 获取详细信息。

*   **业务字段**:
    *   `payment_date`: `EXPRESSION` -> `randomDateInCurrentMonth()`。
    *   `product_customer_new_type`: `LIST_RANDOM` -> `["new", "old"]`。
    *   `business_class_code`: `ENUM_LOOKUP` -> 从 `enum_dictionaries.xlsx` 获取 (Category: `business_class`, Target: `CODE`)。
    *   `business_class`: `ENUM_LOOKUP` -> 根据 code 获取 Name。
    *   `order_type_code`: `ENUM_LOOKUP` -> 从 `enum_dictionaries.xlsx` 获取 (Category: `order_type`, Target: `CODE`)。
    *   `order_type`: `ENUM_LOOKUP` -> 根据 code 获取 Name。

## 3. SQL 模板
*   新增 `cs_order_payment` 模板，包含所有字段。

## 4. 待办提醒
*   请确保 `customer_data.xlsx` 已包含 `customer_manage_id` 列。
*   请确保 `enum_dictionaries.xlsx` 中包含 `business_class` 和 `order_type` 的定义。

确认后将执行上述变更。