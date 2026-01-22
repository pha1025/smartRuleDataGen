# Smart Rule Data Generator 使用手册

## 1. 项目概览

**Smart Rule Data Generator** 是一个基于 Spring Boot 开发的智能测试数据生成工具。它专门解决复杂业务场景下的测试数据构造难题，通过高度可配置的规则引擎，实现数据的批量生成、关联依赖处理以及自动化入库。

### 核心特性

*   **规则驱动**：通过 YAML 文件灵活定义字段生成规则（随机值、枚举、固定值、表达式等）。
*   **依赖注入**：支持字段间的依赖关系（例如：`结束时间` 必须晚于 `开始时间`，`订单金额` 依赖 `商品单価`）。
*   **引用数据集成**：直接读取 Excel 文件作为参考数据源（如客户信息、字典表），支持复杂的查找与关联逻辑。
*   **自动化与集成**：如果不使用 CLI 交互模式，可通过 JVM 参数实现 CI/CD 流水线集成。
*   **多模式输出**：支持生成 SQL 文件导出，也支持直接写入数据库。

---

## 2. 功能使用手册

### 2.1 环境准备

在使用本工具前，请确保环境满足以下要求：

*   **JDK**: Java 17 或更高版本。
*   **Database**: MySQL 8.0+ (或其他支持 JDBC 的数据库)。
*   **Maven**: 3.8+ (用于构建)。

#### 2.1.1 配置文件准备

1.  **Excel 数据文件**：
    确保 `src/main/resources/data` 目录下存在必要的业务数据文件：
    *   `customer_data.xlsx`: 客户基础信息（ID, 名称, 类型）。
    *   `cust_mgr_hierarchy.xlsx`: 客户经理层级结构。
    *   `enum_data.xlsx`: 业务枚举字典。

2.  **应用配置 (`application.yml`)**：
    配置数据库连接字符串、用户名、密码以及 Excel 文件的加载路径。

### 2.2 规则配置指南 (`generator-rules.yml`)

数据生成的核心在于规则配置。以下是常见的规则类型及其用法：

#### 基本规则类型

*   **RANDOM_INT / RANDOM_DOUBLE**: 生成指定范围内的随机数字。
*   **RANDOM_STRING**: 生成随机字符串。
*   **STATIC_VALUE**: 使用固定值。
*   **UUID**: 生成唯一标识符。
*   **DATE**: 生成日期，支持相对于当前时间的偏移，也支持相对于 *其他字段* 的偏移（依赖生成）。
*   **ENUM_LOOKUP**: 从 `enum_data.xlsx` 中随机选取符合条件的枚举值。

#### 高级规则类型

*   **REFERENCE_DATA**: 引用 Excel 数据。
    ```yaml
    - name: "customer_id"
      type: "REFERENCE_DATA"
      refDataType: "CustomerData"  # 对应 Excel 文件加载的数据类型
      lookupKeyField: "type"       # 查找条件字段
      lookupValueSource: "1"       # 查找条件值 (支持静态值或引用其他字段)
      fieldToPopulate: "id"        # 从找到的记录中回填哪个字段
    ```

*   **EXPRESSION**: 使用简单的数学或逻辑表达式。
    ```yaml
    - name: "total_amount"
      type: "EXPRESSION"
      expression: "unit_price * quantity" # 依赖当前记录的其他字段
    ```

### 2.3 运行方式

#### 方式一：命令行交互模式 (CLI)

1.  在项目根目录运行 `mvn spring-boot:run` 或直接运行打包后的 JAR。
2.  控制台将显示欢迎界面，列出可用的数据生成器（如：客户复审数据、交易支付数据等）。
3.  按提示输入生成器序号。
4.  输入生成数量。
5.  等待生成完成，系统将输出结果摘要。

#### 方式二：自动化/静默模式 (CI/CD)

通过传递 JVM 系统参数 `-Dautogen` 来跳过交互环节，直接执行生成任务。

```bash
java -Dautogen="<GeneratorID>,<Count>,<Params>,<ExportSQL>,<InsertDB>" -jar app.jar
```

*   **GeneratorID**: 生成器在列表中的序号（从 1 开始）。
*   **Count**: 生成数据量。
*   **Params**: 额外的键值对参数（可选）。
*   **ExportSQL**: `yes` 或 `no`，是否导出 insert.sql文件。
*   **InsertDB**: `yes` 或 `no`，是否直接写入数据库。

---

## 3. 技术架构手册

### 3.1 系统分层架构

项目遵循标准的 Spring Boot 分层架构，职责清晰：

*   **CLI Layer (`com.smartdata.smartruledatagen.cli`)**:
    *   `DataGenRunner`: 实现了 `CommandLineRunner` 接口。作为程序的入口点，负责解析启动参数，决定是进入交互模式还是自动模式，并调度生成任务。
    
*   **Generator Core (`com.smartdata.smartruledatagen.generator`)**:
    *   `GenericDataGenerator`: 核心引擎。它不包含具体的业务逻辑，而是解析 YAML 规则，根据规则类型分发给不同的处理逻辑。
    *   `handler/*.java`: (逻辑概念) 针对不同 `RuleType` 的具体处理实现。

*   **Service Layer (`com.smartdata.smartruledatagen.service`)**:
    *   `ReferenceDataManager`: 单例服务，负责在启动时加载 Excel 数据到内存，并提供高性能的查询接口（如按 ID 查找、按类型筛选）。
    *   `ExcelDataLoaderService`: 封装 Apache POI，处理底层的 Excel 读取与解析。
    *   `JdbcExecutor`: 封装 JDBC Template，负责构建批量 INSERT 语句并执行数据库操作。

*   **Model Layer (`com.smartdata.smartruledatagen.model`)**:
    *   定义了配置规则的 POJO (`FieldRule`, `GeneratorDefinition`)。
    *   定义了业务数据模型 (`CustomerData`, `CustMgrData`)。

### 3.2 数据流向图

1.  **启动阶段**: `DataGenRunner` 启动 -> `ReferenceDataManager` 加载 Excel -> 缓存参考数据。
2.  **配置加载**: `GeneratorConfigLoader` 读取 YAML -> 解析为 `List<GeneratorDefinition>`。
3.  **生成阶段**:用户选择生成器 -> `GenericDataGenerator` 遍历字段规则 ->
    *   如果是简单规则 -> 直接生成。
    *   如果是依赖规则 -> 等待依赖字段生成后计算。
    *   如果是引用规则 -> 查询 `ReferenceDataManager` 获取数据。
4.  **输出阶段**: 生成的 `Map<String, Object>` 列表 ->
    *   `SqlTemplateRepository` 转换为 SQL 语句 -> 写入文件。
    *   `JdbcExecutor` -> 批量写入数据库。

### 3.3 扩展开发指南

#### 如何添加新的规则类型？

1.  在 `RuleType` 枚举中增加新类型。
2.  在 `FieldRule` 类或其子类中增加必要的配置字段。
3.  在 `GenericDataGenerator.generateValue` 方法的 `switch` 语句中增加处理分支。

#### 如何添加新的 Excel 参考数据？

1.  在 `src/main/resources/data` 添加 Excel 文件。
2.  在 `model` 包下创建对应的 POJO 类。
3.  在 `ExcelDataLoaderService` 中编写解析逻辑。
4.  在 `ReferenceDataManager` 中注册该数据类型的加载与查询方法。

### 3.4 关键类简表

| 类名 | 作用 | 备注 |
| :--- | :--- | :--- |
| `DataGenRunner` | 主控逻辑，CLI 入口 | 核心调度器 |
| `GenericDataGenerator` | 通用生成器 | 规则解析与执行引擎 |
| `ReferenceDataManager` | 静态数据管理 | 内存缓存，高性能查询 |
| `RuleConfig` | YAML 配置映射对象 | 对应 generator-rules.yml |
| `FieldRule` | 单个字段规则定义 | 描述字段如何生成 |

---
*文档生成日期: 2026-01-21*
