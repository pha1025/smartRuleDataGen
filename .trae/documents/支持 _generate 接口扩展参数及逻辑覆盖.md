## 1. 服务层功能增强
- **[ReferenceDataManager.java](file:///d:/Workspace/smartRuleDataGen/src/main/java/com/smartdata/smartruledatagen/service/ReferenceDataManager.java)**:
    - 新增 `getCustomersByManageId(String manageId)` 方法，用于从内存中检索归属于指定客户经理的所有客户。
    - 确保 `getCustomerById(String id)` 方法逻辑完善，以便根据 ID 快速获取客户基础信息。

## 2. 控制器逻辑调整 (处理 extraParams)
- **[DataGenController.java](file:///d:/Workspace/smartRuleDataGen/src/main/java/com/smartdata/smartruledatagen/controller/DataGenController.java)**:
    - 在 `generateData` 方法中，从 `request.getExtraParams()` 中提取以下可选参数：
        - `customerManageId` (String)
        - `customerIds` (List<String> 或 String，支持解析)
        - `endDateMonth` (String)
    - **更新筛选逻辑**:
        1. **情况 A (ManageID + CustomerIDs)**: 如果 `extraParams` 中同时包含这两个参数，直接构造 `CustomerData` 列表。此时跳过从文件/内存查询的校验步骤，每个客户对象设置对应的 `customerId` 和 `customerManageId`。
        2. **情况 B (仅 ManageID)**: 如果只有 `customerManageId`，调用 `referenceDataManager.getCustomersByManageId` 获取客户。
        3. **情况 C (无以上参数)**: 保留现有的基于 `regionCode` 的筛选逻辑。
    - 确保 `endDateMonth` 被透传到生成器参数 Map 中。

## 3. 生成引擎逻辑增强
- **[GenericDataGenerator.java](file:///d:/Workspace/smartRuleDataGen/src/main/java/com/smartdata/smartruledatagen/generator/GenericDataGenerator.java)**:
    - **日期生成覆盖**: 在 `generateFieldValue` 中，若字段类型为 `DateFieldRule` 且 `params` 中存在 `endDateMonth`，则不再执行随机偏移或当前时间逻辑，而是直接返回 `endDateMonth`（针对 `yyyy-MM-dd` 格式自动补全为 1 号）。
    - **占位符增强**: 优化 `generateSqls` 方法，使其在替换生成的字段值后，再遍历 `params` 中的所有键值对（包括 `endDateMonth` 等），将 SQL 模板中匹配的 `{key}` 占位符进行替换。

## 4. 文档更新
- **[manual_zh.md](file:///d:/Workspace/smartRuleDataGen/docs/manual_zh.md)**: 在 `/generate` 接口文档中，明确 `extraParams` 支持 `endDateMonth`、`customerManageId` 和 `customerIds` 参数，并说明其覆盖原有规则的逻辑。

## 验证计划
- 发送 `POST /generate` 请求，在 `extraParams` 中携带 `customerManageId` 和 `customerIds`，验证生成的 SQL 是否准确包含这些 ID 且未触发文件查找。
- 发送带有 `endDateMonth` 的请求，验证所有日期字段是否已固定为该月份，且 SQL 模板中的对应占位符已生效。