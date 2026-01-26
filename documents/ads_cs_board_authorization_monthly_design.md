# 新增生成器 `ads_cs_board_authorization_monthly` 设计方案

## 1. 需求分析

### 1.1 目标表信息
*   **表名**: `cctest_not_delete.ads_cs_board_authorization_monthly`
*   **数据库**: `db2`
*   **SQL 示例**:
    ```sql
    INSERT INTO cctest_not_delete.ads_cs_board_authorization_monthly 
    (true_id, `month`, agent_id, agent_name, sale_area_id, agent_user_code, num) 
    VALUES 
    ('DF910C299FBE1BDD4AC92093ECC77B2A', '2025-06', 'F12ADE025BFD428880B327BAA6A92993', '新模型主从客户主机构', 752, '2NU8S32U', 0)
    ```

### 1.2 字段映射与规则
| 字段名 | 来源/规则 | 说明 |
| :--- | :--- | :--- |
| `true_id` | `customer_manage_id` | 客户经理ID，需受 `big_region_code` 限制（通过关联客户隐式限制） |
| `month` | `DATE` | 默认当月，格式 `yyyy-MM` |
| `agent_id` | `customer_id` | 客户ID，**限制 Customer Type 为 2 或 3** |
| `agent_name` | `customer_name` | 客户名称 |
| `sale_area_id` | `CustomerData.saleAreaId` | **新增字段**，来源 `customer_data.xlsx` |
| `agent_user_code` | `CustomerData.servyouNum` | **新增字段**，来源 `customer_data.xlsx` |
| `num` | `RANDOM_INT` | 随机数字 |

### 1.3 外部依赖变更
*   **Excel**: `customer_data.xlsx` 需新增两列：`servyou_num` (第5列) 和 `sale_area_id` (第6列)。

---

## 2. 详细代码变更设计

### 2.1 Java 模型类变更

**文件**: `src/main/java/com/smartdata/smartruledatagen/model/CustomerData.java`

需增加 `servyouNum` 和 `saleAreaId` 字段。

```java
package com.smartdata.smartruledatagen.model;

import lombok.Data;

@Data
public class CustomerData {
    private String customerId;       // 索引 0
    private String customerName;     // 索引 1
    private String customerType;     // 索引 2
    private String customerManageId; // 索引 3
    
    // 新增字段
    private String servyouNum;       // 索引 4 (对应 agent_user_code)
    private String saleAreaId;       // 索引 5 (对应 sale_area_id)
}
```

### 2.2 Excel 加载服务变更

**文件**: `src/main/java/com/smartdata/smartruledatagen/service/ExcelDataLoaderService.java`

更新 `loadCustomerData` 方法以读取新增的列。

```java
// 在 loadCustomerData 方法的循环中
CustomerData data = new CustomerData();
data.setCustomerId(getCellValue(row.getCell(0)));
data.setCustomerName(getCellValue(row.getCell(1)));
// ... (原有逻辑保持不变，设置 type 和 manageId)

// 新增读取逻辑
data.setServyouNum(getCellValue(row.getCell(4))); // 假设在第5列
data.setSaleAreaId(getCellValue(row.getCell(5))); // 假设在第6列

dataList.add(data);
```

### 2.3 SQL 模板注册

**文件**: `src/main/java/com/smartdata/smartruledatagen/service/SqlTemplateRepository.java`

注册新的 SQL 模板。

```java
// 在构造函数中添加
templates.put("cs_board_auth_monthly",
        "INSERT INTO cctest_not_delete.ads_cs_board_authorization_monthly " +
        "(true_id, `month`, agent_id, agent_name, sale_area_id, agent_user_code, num) " +
        "VALUES " +
        "({true_id}, {month}, {agent_id}, {agent_name}, {sale_area_id}, {agent_user_code}, {num})");
```

### 2.4 控制器逻辑确认

**文件**: `src/main/java/com/smartdata/smartruledatagen/controller/DataGenController.java`

需要在生成前将新增的字段 (`servyouNum`, `saleAreaId`) 放入预置数据 (`preDefinedRecords`) 中，以便规则引擎可以直接引用。同时，**新增对 Customer Type 的过滤逻辑**。

```java
// 1. 在生成数据前的过滤逻辑中
// 针对 csBoardAuthMonthly 生成器，筛选 type=2 或 type=3 的客户
if ("csBoardAuthMonthly".equals(request.getGeneratorName())) {
    candidates = candidates.stream()
            .filter(c -> "2".equals(c.getCustomerType()) || "3".equals(c.getCustomerType()))
            .collect(Collectors.toList());
}

// 2. 在构建 preDefinedRecords 的循环中
if (customer.getServyouNum() != null) {
    record.put("servyou_num", customer.getServyouNum());
}
if (customer.getSaleAreaId() != null) {
    record.put("sale_area_id", customer.getSaleAreaId());
}
// 原有的 customer_manage_id 逻辑已存在
```

---

## 3. 配置文件变更

**文件**: `src/main/resources/generator-rules.yml`

新增生成器配置。

```yaml
  csBoardAuthMonthly:
    tableName: "cctest_not_delete.ads_cs_board_authorization_monthly"
    dbKey: "db2" # 注意：需确认 application.yml 中配置了 db2 数据源
    sqlTemplateKey: "cs_board_auth_monthly"
    fields:
      - name: "true_id"
        type: "EXPRESSION"
        sqlType: "VARCHAR"
        expression: "customer_manage_id" # 直接引用预置的客户经理ID
        nullable: false
      - name: "month"
        type: "DATE"
        sqlType: "VARCHAR" # SQL中可能是字符串格式
        format: "yyyy-MM"
        baseDateSource: "PARAM"
        randomOffsetDaysMin: 0
        randomOffsetDaysMax: 0 # 锁定为当月
        nullable: false
      - name: "agent_id"
        type: "EXPRESSION"
        sqlType: "VARCHAR"
        expression: "customer_id" # 引用预置的客户ID
        nullable: false
      - name: "agent_name"
        type: "EXPRESSION"
        sqlType: "VARCHAR"
        expression: "customer_name" # 引用预置的客户名称
        nullable: false
      - name: "sale_area_id"
        type: "EXPRESSION"
        sqlType: "INT" # 假设是数字
        expression: "sale_area_id" # 引用 CustomerData 中的新字段
        nullable: true
      - name: "agent_user_code"
        type: "EXPRESSION"
        sqlType: "VARCHAR"
        expression: "servyou_num" # 引用 CustomerData 中的新字段
        nullable: true
      - name: "num"
        type: "RANDOM_INT"
        sqlType: "INT"
        min: 0
        max: 100
        nullable: false
```

---

## 4. Excel 数据结构变更说明

请按照以下结构更新 `src/main/resources/data/customer_data.xlsx`：

| 列索引 | 字段名 | 示例值 | 说明 |
| :--- | :--- | :--- | :--- |
| 0 | customer_id | CUST001 | 现有 |
| 1 | customer_name | 测试客户A | 现有 |
| 2 | customer_type | 1 | 现有 (或根据Sheet名) |
| 3 | customer_manage_id | MGR001 | 现有 |
| **4** | **servyou_num** | **2NU8S32U** | **新增** (对应 `agent_user_code`) |
| **5** | **sale_area_id** | **752** | **新增** (对应 `sale_area_id`) |

---

## 5. 总结

该方案通过扩展现有的 `CustomerData` 模型和 Excel 加载逻辑，将新增的业务字段纳入数据池。利用现有的预置数据机制 (`preDefinedRecords`)，将这些字段直接暴露给规则引擎。
特别地，在 `DataGenController` 中增加了对 `csBoardAuthMonthly` 生成器的**前置过滤逻辑**，确保只生成 `Customer Type` 为 `2` 或 `3` 的数据。
