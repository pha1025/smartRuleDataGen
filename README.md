src/main/java/com/example/testdatagenerator
├── TestDataGenApplication.java
├── cli
│   └── DataGenRunner.java             # 命令行入口
├── config
│   ├── DataSourceConfig.java          # 多数据源配置 (不变)
│   └── GeneratorConfigLoader.java     # 新增：加载 YAML 规则
├── generator
│   ├── GenericDataGenerator.java      # 新增：核心通用数据生成器
│   └── util                           # 新增：辅助工具类
│       ├── ExpressionEvaluator.java   # 简易表达式求值器
│       └── ValueFormatter.java        # 格式化数据为 SQL 兼容字符串
├── model
│   ├── CustMgrData.java               # 客户经理数据模型 (不变)
│   ├── EnumMapping.java               # 枚举映射数据模型 (不变)
│   └── rules                          # 新增：YAML 规则对应的 POJOs
│       ├── FieldRule.java
│       ├── GeneratorDefinition.java
│       ├── RuleConfig.java
│       ├── RuleType.java
│       └── ... (其他 FieldRule 的实现类)
├── service
│   ├── database
│   │   └── JdbcExecutor.java          # 数据库执行器 (不变)
│   ├── ExcelDataLoaderService.java    # Excel 数据加载器 (不变)
│   ├── ReferenceDataManager.java      # 参考数据管理器 (更新：提供更多查询接口)
│   └── SqlTemplateRepository.java     # SQL 模板仓库 (不变)
└── util
└── DateUtil.java                  # 日期工具 (不变)