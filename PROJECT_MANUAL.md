# Smart Rule Data Generator - 项目手册

## 1. 项目概览

本项目是一个基于 **Spring Boot** 的智能测试数据生成工具，旨在通过**配置化**的方式灵活生成复杂的业务测试数据，并支持生成 SQL 脚本及直接入库。

### 核心功能
- **规则驱动生成**：通过 YAML 配置文件定义数据生成规则（随机、枚举、引用、表达式等）。
- **自动化支持**：支持 CLI 交互模式和 CI/CD 自动化模式。
- **依赖处理**：自动解决字段间的生成依赖（如 B 字段依赖 A 字段的值）。
- **引用数据管理**：支持从 Excel 加载参考数据（如客户经理层级、枚举字典、客户信息），并在生成过程中动态引用。
- **多数据源支持**：支持向不同的数据库实例批量插入数据。

---

## 2. 项目结构

### 2.1 核心组件
*   **`DataGenRunner`**: 程序入口，负责流程控制（交互/自动）、数据生成调度、文件导出和 SQL 执行。
*   **`GenericDataGenerator`**: 核心生成引擎，解析 YAML 规则，调用各种 Rule Handler 生成数据。
*   **`ReferenceDataManager`**: 参考数据管理器，负责加载 Excel 数据并在内存中提供高效查询。
*   **`ExcelDataLoaderService`**: 负责解析 Excel 文件（POI）。
*   **`SqlTemplateRepository`**: 管理 SQL 插入模板。
*   **`JdbcExecutor`**: 封装 JDBC 操作，支持批量插入。

### 2.2 目录结构
```text
src/main/java/com/smartdata/smartruledatagen/
├── cli/                # 命令行交互层
├── config/             # 配置加载 (YAML, DataSource)
├── generator/          # 生成逻辑核心 (各种 Rule 实现)
├── model/              # 数据模型 (POJO)
├── service/            # 业务服务 (Excel加载, SQL模板, 引用数据管理)
└── util/               # 工具类
```

---

## 3. 使用手册

### 3.1 环境准备
1.  **Excel 数据文件**：
    确保 `src/main/resources` (或 `src/main/resources/data`) 目录下存在以下文件：
    - `cust_mgr_hierarchy.xlsx`: 客户经理层级数据。
    - `enum_data.xlsx`: 枚举字典数据。
    - **`customer_data.xlsx`** (新增): 客户基础信息数据 (路径: `src/main/resources/data/customer_data.xlsx`)。

2.  **配置文件** (`application.yml`)：
    配置数据库连接信息和 Excel 文件路径。

### 3.2 运行方式

#### 方式一：交互式运行
直接启动项目，根据控制台提示选择生成器并输入数量。

#### 方式二：自动化运行
使用 `-Dautogen` 参数：
```bash
java -Dautogen="1,100,,yes,yes" -jar target/smartRuleDataGen-0.0.1-SNAPSHOT.jar
```
参数格式：`生成器序号,数量,参数KV,是否导出SQL,是否入库`

---

## 4. 功能拓展：客户数据 Excel 维护与关联

本次更新增加了对客户数据 (`customer_id`, `customer_name`) 的 Excel 维护支持，并实现了基于 `customer_type` 的自动关联逻辑。

### 4.1 新增功能说明
1.  **Excel 维护**：
    新建 `src/main/resources/data/customer_data.xlsx`，用于维护客户信息。
    **Excel 格式要求**（Sheet1）：
    | 第一列 (ID) | 第二列 (Name) | 第三列 (Type) |
    | :--- | :--- | :--- |
    | CUST001 | 杭州阿里巴巴 | 1 |
    | CUST002 | 北京字节跳动 | 2 |
    | CUST003 | 深圳腾讯 | 1 |

2.  **自动关联逻辑**：
    - 程序内部已实现 `CustomerData` 模型及其加载逻辑。
    - 生成器可以通过配置 `REFERENCE_DATA` 规则，指定筛选特定类型的客户。
    - **默认绑定规则**：
        - **生成器 1 & 2** (如 `customerReviewExpire`, `tradeKpiPayment`)：默认筛选 `customer_type = 1` 的客户。
        - **生成器 3** (如 `tradeKpiPaymentDetail`)：默认筛选 `customer_type = 2` (或 3) 的客户。

### 4.2 配置示例 (`generator-rules.yml`)

#### 场景 A：绑定 Type = 1 的客户（生成器 1 和 2）
```yaml
      - name: "customer_id"
        type: "REFERENCE_DATA"
        refDataType: "CustomerData"
        lookupKeyField: "type"
        lookupValueSource: "1"  # <--- 指定筛选 Type = 1
        fieldToPopulate: "id"
      - name: "customer_name"
        type: "REFERENCE_DATA"
        refDataType: "CustomerData"
        lookupKeyField: "id"
        lookupValueSource: "customer_id" # <--- 依赖生成的 ID
        fieldToPopulate: "name"
        dependsOn: "customer_id"
```

#### 场景 B：绑定 Type = 2 的客户（生成器 3）
```yaml
      - name: "customer_id"
        type: "REFERENCE_DATA"
        refDataType: "CustomerData"
        lookupKeyField: "type"
        lookupValueSource: "2"  # <--- 指定筛选 Type = 2
        fieldToPopulate: "id"
```

### 4.3 代码实现细节
- **`CustomerData.java`**: 新增模型类，包含 id, name, type。
- **`ExcelDataLoaderService`**: 增加了 `loadCustomerData` 方法解析 Excel。
- **`ReferenceDataManager`**: 增加了 `customerDataByType` 索引，支持按 Type 快速查找客户列表。
- **`GenericDataGenerator`**: 扩展了 `REFERENCE_DATA` 处理逻辑，支持 `CustomerData` 的 `type->id` 和 `id->name` 查找模式。

---

## 5. 待办事项
- 请务必创建 `src/main/resources/data/customer_data.xlsx` 文件并填充测试数据，否则生成过程会因找不到客户数据而使用默认值 `CUST_DEFAULT`。
