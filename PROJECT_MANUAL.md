# Smart Rule Data Generator - 项目手册

## 1. 项目概览

本项目是一个基于 **Spring Boot** 的智能测试数据生成工具，旨在通过**配置化**的方式灵活生成复杂的业务测试数据，并支持生成 SQL 脚本及直接入库。

### 核心功能
- **规则驱动生成**：通过 YAML 配置文件定义数据生成规则（随机、枚举、引用、表达式等）。
- **Web API 接口**：提供 RESTful 接口触发数据生成，便于集成测试平台。
- **多表关联生成**：支持一个生成器同时生成主从表数据（如订单主表+详情表），保持字段一致性。
- **自动化支持**：支持 CLI 交互模式和 CI/CD 自动化模式。
- **依赖处理**：自动解决字段间的生成依赖（如 B 字段依赖 A 字段的值）。
- **引用数据管理**：支持从 Excel 加载参考数据（如客户经理层级、枚举字典、客户信息），并在生成过程中动态引用。
- **多数据源支持**：支持向不同的数据库实例批量插入数据。

---

## 2. 项目结构

### 2.1 核心组件
*   **`DataGenRunner`**: 程序入口，负责流程控制（交互/自动）。
*   **`DataGenController`**: Web 接口层，处理数据生成请求。
*   **`GenericDataGenerator`**: 核心生成引擎，解析 YAML 规则，调用各种 Rule Handler 生成数据。支持多模板 SQL 生成。
*   **`ReferenceDataManager`**: 参考数据管理器，负责加载 Excel 数据并在内存中提供高效查询。
*   **`ExcelDataLoaderService`**: 负责解析 Excel 文件（POI），支持多 Sheet 页解析。
*   **`SqlTemplateRepository`**: 管理 SQL 插入模板，支持命名占位符 `{fieldName}`。
*   **`ExpressionEvaluator`**: 强大的表达式引擎，支持变量替换、算术运算及自定义函数（如 `randomCustMgrInRegion`）。

### 2.2 目录结构
```text
src/main/java/com/smartdata/smartruledatagen/
├── cli/                # 命令行交互层
├── config/             # 配置加载 (YAML, DataSource)
├── controller/         # Web 控制层
├── generator/          # 生成逻辑核心 (各种 Rule 实现)
├── model/              # 数据模型 (POJO)
├── service/            # 业务服务 (Excel加载, SQL模板, 引用数据管理)
└── util/               # 工具类
```

---

## 3. 使用手册

### 3.1 环境准备
1.  **Excel 数据文件**：
    确保 `src/main/resources/data` 目录下存在以下文件：
    - **`cust_mgr_hierarchy.xlsx`**: 客户经理层级数据。
        - **结构变更**：支持多 Sheet 页，**Sheet 名称即为 Big Region Code**（如 `004012020`）。
    - **`customer_data.xlsx`**: 客户基础信息数据。
        - **结构变更**：支持多 Sheet 页，**Sheet 名称即为 Customer Type**（如 `1`, `2`）。
        - 包含列：`customer_id`, `customer_name`, `customer_type` (可选), `customer_manage_id`。
    - **`region_province.xlsx`**: 大区与省份对应关系。
    - `enum_dictionaries.xlsx`: 枚举字典数据。

2.  **配置文件** (`application.yml` & `generator-rules.yml`)：
    配置数据库连接信息和生成规则。

### 3.2 运行方式

#### 方式一：Web API 调用 (推荐)
启动服务后，访问 `http://localhost:8081`。
**接口**: `POST /api/datagen/generate`
**请求体**:
```json
{
    "generatorName": "tradeKpiPayment",
    "count": 10,
    "regionCode": "004012020",
    "executeInsert": false
}
```
**逻辑说明**：
- 自动校验 `regionCode` 是否在 Excel 中存在（通过 Sheet 页判断）。
- 对于 `tradeKpiPayment`，会自动筛选 `customer_type=1` 的客户数据。
- `generatedCount` 返回实际生成的业务记录数（即使底层生成了多条 SQL）。

#### 方式二：CLI 交互
直接启动项目，根据控制台提示选择生成器并输入数量。

---

## 4. 功能特性与配置

### 4.1 多表同时生成
生成器现在支持定义 `extraSqlTemplateKeys`，允许一次生成操作产出多条关联的 SQL 语句。
**场景**：生成“交易KPI支付”数据时，同时插入主表 (`..._cust_rt`) 和详情表 (`..._detail_rt`)。

**配置示例** (`generator-rules.yml`)：
```yaml
  tradeKpiPayment:
    sqlTemplateKey: "trade_kpi_payment"
    extraSqlTemplateKeys: ["trade_kpi_payment_detail"] # 关联的额外模板
    fields:
      # 主表字段
      - name: "zx_payment_amount"
        type: "RANDOM_DOUBLE"
        precision: 2 # 保留2位小数
        # ...
      # 详情表字段 (与主表字段混合定义，通过 SQL 模板占位符区分)
      - name: "order_id"
        type: "EXPRESSION"
        expression: "generateDetailOrderId()"
```

**SQL 模板** (`SqlTemplateRepository.java`)：
使用 `{fieldName}` 命名占位符，实现字段在不同表之间的灵活映射。
```sql
INSERT INTO ... VALUES ({payment_date}, {order_id}, {zx_payment_amount})
```

### 4.2 增强的字段规则
- **`RANDOM_DOUBLE`**: 支持 `precision` 属性，精确控制小数位数（如金额字段）。
- **`DATE`**: 支持 `randomOffsetDaysMax: 0`，可限制生成日期不大于基准日期（当前日期）。
- **`REFERENCE_DATA`**: 增强了对 Excel 数据的查找能力。

### 4.3 表达式引擎函数
新增了专门用于业务逻辑的函数：
- `randomCustMgrInRegion(regionCode, fieldName)`: 从指定大区随机获取一个客户经理的属性（用于生成 `signer_id` 等）。
- `generateDetailOrderId()`: 生成特定格式的订单号。

### 4.4 数据关联逻辑
- **客户与经理关联**：系统自动加载 `customer_data.xlsx` 中的 `customer_manage_id`，并与 `cust_mgr_hierarchy.xlsx` 中的客户经理建立映射。
- **区域校验**：接口层直接利用内存中的 Sheet 页索引校验区域代码的有效性。

---

## 5. 维护指南
- **新增区域**：只需在 `cust_mgr_hierarchy.xlsx` 中新建一个 Sheet，名称为区域代码，填入该区域的客户经理数据即可。
- **新增客户类型**：在 `customer_data.xlsx` 中新建 Sheet，名称为类型代码。
- **调整规则**：修改 `src/main/resources/generator-rules.yml`，无需重新编译代码（部分静态资源需重启生效）。
