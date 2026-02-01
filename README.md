# Smart Rule Data Generator

这是一个基于 **Spring Boot** 的智能测试数据生成工具，旨在通过**配置化**的方式灵活生成复杂的业务测试数据，并支持生成 SQL 脚本及直接入库。

## 项目结构

```text
src/main/java/com/smartdata/smartruledatagen/
├── SmartRuleDataGenApplication.java    # 项目启动类
├── TestDataGenApplication.java         # 测试启动类/备用入口
├── cli/
│   └── DataGenRunner.java             # 命令行入口，支持交互与自动模式
├── config/
│   ├── CorsConfig.java                # 跨域资源共享配置
│   ├── DataSourceConfig.java          # 多数据源连接配置
│   └── GeneratorConfigLoader.java     # 加载并解析 YAML 生成规则
├── controller/
│   └── DataGenController.java         # Web API 接口层，处理远程生成请求
├── dto/
│   ├── DataGenRequest.java            # 数据生成请求对象
│   └── DataGenResponse.java           # 数据生成响应对象
├── generator/
│   ├── AbstractTableGenerator.java    # 数据生成器抽象基类
│   ├── GenericDataGenerator.java      # 核心通用数据生成引擎
│   ├── CustomerReviewGenerator.java   # 客户复审业务专属生成器
│   ├── TradeKpiPaymentGenerator.java  # 交易KPI支付业务专属生成器
│   ├── exception/
│   │   └── DependencyNotMetException.java # 字段依赖循环或缺失异常
│   └── util/
│       ├── ExpressionEvaluator.java   # 表达式求值器 (支持函数与变量)
│       └── ValueFormatter.java        # SQL 兼容性字段格式化工具
├── model/
│   ├── CustMgrData.java               # 客户经理层级数据模型
│   ├── CustomerData.java              # 客户基础信息模型
│   ├── EnumMapping.java               # 枚举字典映射模型
│   ├── RegionProvinceData.java        # 行政区域省份模型
│   └── rules/                         # 规则配置对应的 POJO 体系
│       ├── FieldRule.java             # 字段生成规则基类
│       ├── GeneratorDefinition.java    # 单个生成器定义
│       ├── RuleConfig.java            # 全局规则配置根对象
│       └── ...                        # 各类具体规则实现 (Random, Date, Ref 等)
├── service/
│   ├── database/
│   │   └── JdbcExecutor.java          # 封装 JDBC 执行批量插入与 SQL 生成
│   ├── ExcelDataLoaderService.java    # 基于 POI 的 Excel 数据加载服务
│   ├── ReferenceDataManager.java      # 参考数据管理器，提供内存级高速查询
│   └── SqlTemplateRepository.java     # SQL 模板仓库，管理各表的 INSERT 语句
└── util/
    └── DateUtil.java                  # 日期与时间处理工具类

src/main/resources/
├── data/                              # 存放参考数据 Excel 文件 (客户、经理、枚举等)
├── application.yml                    # Spring Boot 应用基础配置 (数据库、端口等)
└── generator-rules.yml                # 核心业务数据生成规则定义
```

## 核心功能

- **规则驱动生成**：通过 YAML 定义随机、枚举、引用、表达式等多种生成规则。
- **多表关联生成**：支持主从表（如订单+详情）同步生成，保持字段间的一致性。
- **参考数据集成**：自动加载 Excel 数据作为生成过程中的上下文参考。
- **双模运行**：支持 CLI 命令行交互和标准 RESTful API 调用。
- **依赖自动处理**：引擎自动识别并解决字段间的生成顺序依赖。

## 快速开始

### 1. 环境准备
- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 2. 运行项目
```bash
mvn spring-boot:run
```

### 3. 调用 API
```bash
curl -X POST http://localhost:8081/api/datagen/generate \
-H "Content-Type: application/json" \
-d '{
    "generatorName": "tradeKpiPayment",
    "count": 10,
    "regionCode": "004012020",
    "executeInsert": false
}'
```

---
*更多详细信息请参考 [PROJECT_MANUAL.md](file:///d:/Workspace/smartRuleDataGen/PROJECT_MANUAL.md)*
