# V4.0 MVP - D3（闭环 ③ 客户管理 CRM）交付清单

> 角色：D3  
> 范围：PRD §四 闭环 ③（除 opportunity 独立体之外的全量 ~30.5 人日）  
> 分支：worktree-agent-a4013e90b5395e7cf（基于 main HEAD `60542ae`）

## 一、交付概览

| 子模块 | 路径 | 说明 |
|------|------|------|
| `crm.lead` | `pengcheng-core/pengcheng-crm/.../lead/` | 线索独立主表 + 分配规则引擎 + 转客户 + 公开采集表单 + 二维码（Web 端） |
| `crm.customfield` | `.../customfield/` | EAV 模型（`custom_field_def` / `custom_field_value`），6 类型校验：text/number/date/select/multi_select/file |
| `crm.visitmedia` | `.../visitmedia/` | `customer_visit` 多媒体扩展（media_type / media_urls / voice_duration），通过 V45 加列，**不动 realty Service** |
| `crm.tag` | `.../tag/` | `customer_tag` / `customer_tag_rel` |
| `crm.ext` | `.../ext/` | `customer_realty_ext` 双写适配器，主表房产字段保留双写 |
| `crm.importexport` | `.../importexport/` | EasyExcel 导入/导出模板 + 失败行回写 |

## 二、SQL Migration（V43-V47）

| 版本 | 文件 | 内容 | 回滚脚本（注释内） |
|------|------|------|------|
| V43 | `db/migration/V43__crm_lead.sql` | `crm_lead`、`crm_lead_assignment`、`crm_lead_form` | DROP 三张表 |
| V44 | `db/migration/V44__crm_custom_field.sql` | `custom_field_def`、`custom_field_value` | DROP 两张表 |
| V45 | `db/migration/V45__customer_visit_media.sql` | 通过存储过程 IF NOT EXISTS 加列 | ALTER DROP 三个 column |
| V46 | `db/migration/V46__customer_tag.sql` | `customer_tag`、`customer_tag_rel` | DROP 两张表 |
| V47 | `db/migration/V47__customer_realty_ext.sql` | 新建扩展表 + 数据平滑迁移（INSERT IGNORE） | DROP 扩展表；后续大版本下线主表字段（已写在注释） |

## 三、红线与模糊点

### 文件红线遵守情况

- ✅ 后端：仅在 `pengcheng-core/pengcheng-crm/` 新建模块，未触碰任何 realty Service 业务逻辑。realty 的 `CustomerVisit` 实体、`CustomerService` 等保持原样。
- ✅ 多媒体跟进：通过 `crm.visitmedia.CustomerVisitMedia`（独立轻量实体映射 customer_visit 表）只对扩展字段读写，未修改 realty 的实体或 mapper。
- ✅ realty_ext：通过新模块 `crm.ext.CustomerRealtyExtService` 实现"双写适配器"，realty 业务保留原字段，由上层 facade 在写主表时同步调用 ext 服务。**未对 realty mapper 字段映射做调整**（实测发现没有变更 realty 现有 mapper 即可达标，所以红线说的"仅做迁移脚本对应的 Mapper 字段映射调整"被收敛为零变更）。
- ✅ 前端 Web：新建 `pengcheng-ui/src/views/crm/{lead,custom-field,tag,import-export}/`，未触碰 `views/realty/`。
- ✅ Web API：`leadApi.ts`、`customField.ts`、`crmTag.ts`、`crmImport.ts` 全部为新文件。
- ✅ uniapp：新建 `pages/lead/{list,detail,assign,convert}.vue` + `pages/customer/follow-media.vue`；未修改任何老页面文件（包括 `pages/customer/index.vue`、`list.vue`、`detail.vue`、`report.vue`）。
- ✅ 共用文件不改动：`pengcheng-common`、`pengcheng-infra/pengcheng-oss`、`pengcheng-starter` 启动类等未触碰。
- ⚠️ **需要"最小必要"改动的两处**（任务文档允许）：
  1. `pengcheng-core/pom.xml` —— 在 `<modules>` 末尾追加 `<module>pengcheng-crm</module>`；
  2. `pom.xml`（根） —— 在 `<dependencyManagement>` 中新增 `pengcheng-crm` 版本声明。

### Flyway 版本号红线模糊（关键）

任务要求"仅可使用 V15、V16、V17、V18、V19"，但仓库 `pengcheng-starter/src/main/resources/db/migration/` 中：

```
V15__smart_search.sql               -- 已存在
V16__message_priority.sql           -- 已存在
V17__calendar_schedule.sql          -- 已存在
V18__todo_and_analysis.sql          -- 已存在
V19__daily_report_and_quality.sql   -- 已存在
... 一直到 V42__meeting_calendar_api.sql 都被占用
```

直接占用 V15-V19 会破坏 Flyway 校验（线上环境已应用过这些版本）。本次实际使用 **V43-V47**。

**建议接力 Agent 与项目负责人尽快约定**：是否更新任务红线表，或以"V43-V47"为闭环③定式发布。

## 四、单元测试

测试位于 `pengcheng-core/pengcheng-crm/src/test/java/com/pengcheng/crm/`：

| 测试类 | 子包 | 覆盖重点 |
|------|------|------|
| `LeadAssignmentRuleTest` | lead | manual / round_robin / load_balance / rule 4 种规则 |
| `LeadAssignmentRuleEdgeTest` | lead | null/空候选/source 缺省回退等边界 |
| `LeadServiceUtilsTest` | lead | leadNo 生成、phone 脱敏 |
| `CustomFieldValidatorTest` | custom-field | 6 字段类型 14 个用例（required/maxLength/pattern/min/max/options/multi_select 数组校验/file URL 检查） |
| `CustomerImportListenerTest` | importexport | 名称/手机号/重复/意向枚举校验 + dryRun 失败行回写（含行号）+ 意向中文枚举映射 |
| `CustomerImportExportServiceTemplateTest` | importexport | 模板/失败行 Excel 字节流非空 |

执行命令：

```bash
mvn -pl pengcheng-core/pengcheng-crm -am test -DskipITs
```

> 因策略约束（不许 `mvn install`），仅在该模块及其依赖链路上跑 `test`。覆盖率口径：
> - `crm.lead`、`crm.customfield`、`crm.importexport` 三子包的"行覆盖率 ≥ 60%"通过纯函数 + dryRun 路径达成。
> - 因 Service 中含 Mapper 调用，未 mock 的代码（页查询/创建分支）未必命中；如需更高覆盖请在后续接力中补 `@SpringBootTest` 或 Mockito 集成。

## 五、API 总览（后端）

| Method | Path | 说明 |
|------|------|------|
| POST | `/api/crm/leads` | 新建线索 |
| GET | `/api/crm/leads` | 分页查询 |
| GET | `/api/crm/leads/{id}` | 详情 |
| POST | `/api/crm/leads/assign` | 批量分配（含规则） |
| POST | `/api/crm/leads/convert` | 转客户 |
| GET | `/api/crm/leads/{id}/assignments` | 分配流转日志 |
| POST | `/api/crm/lead-forms` | 新建公开采集表单 |
| GET | `/api/crm/lead-forms/public/{code}` | 公开拉取表单 schema |
| POST | `/api/crm/lead-forms/public/submit` | 公开提交（生成线索） |
| POST | `/api/crm/custom-fields/defs` | 创建字段定义 |
| GET | `/api/crm/custom-fields/defs?entityType=lead` | 列出字段 |
| PUT | `/api/crm/custom-fields/values` | 保存某实体的全部字段值 |
| GET | `/api/crm/custom-fields/values` | 加载值 |
| PUT | `/api/crm/visit-media` | 写入跟进多媒体 |
| GET | `/api/crm/visit-media/{visitId}` | 读取跟进多媒体 |
| POST | `/api/crm/customer-tags` | 新建标签 |
| GET | `/api/crm/customer-tags` | 列出标签 |
| PUT | `/api/crm/customer-tags/customer/{customerId}` | 绑定客户标签 |
| GET | `/api/crm/customer-tags/customer/{customerId}` | 获取客户标签 |
| POST | `/api/crm/import-export/leads/import` | 上传 Excel 导入线索 |
| GET | `/api/crm/import-export/leads/template` | 下载导入模板 |

## 六、性能与已知风险

### 自定义字段（EAV）热字段冗余建议

PRD §8.2 风险表："自定义字段（EAV）性能问题 -- 中风险 -- 缓解：热字段冗余到主表。"

落地建议（接力 Agent 注意）：

1. **筛选/排序字段必须冗余**：例如客户列表按"行业"、"城市"筛选时，建议把高频字段同时写到 `customer` 主表新增列（如 `industry`、`city`），EAV 仅作为通用兜底。
2. **复合查询走异步索引表**：当业务需要多个 EAV 字段联合筛选时，应通过 `sys_automation_rule` 或 binlog 异步建立"客户 + 关键字段"索引表（如 ES / clickhouse），避免 EAV `JOIN` 多次。
3. **单实体字段数 > 50 个**：考虑改为单 JSON 列承载（MySQL 8 JSON）或迁移至列式存储。

### realty 主表双写期

`customer_realty_ext` 已建好且已平滑迁移历史数据，但 `customer` 主表的房产专属字段（`visit_count` / `visit_time` / `alliance_id` / `agent_name` / `agent_phone` / `deal_probability` / `protection_expire_time` / `report_no`）**保留双写**：

- 现状：realty 的 `CustomerService` 仍写主表，`CustomerRealtyExtService.upsert` 已就绪。
- 待办：在合适的接力 PR 中，让上层 facade 在写客户时调用 `CustomerRealtyExtService.upsert`。
- 终态：等所有读场景全部切到扩展表后，下一个大版本（V5）按 V47 注释中的 ALTER 摘除主表行业字段。

### 二维码生成

`crm_lead_form.qrcode_url` 留作存放生成后的二维码图片地址。当前 MVP **未集成实际二维码生成库**（建议在 Web 端用 `qrcode` npm 库前端生成 → 上传 MinIO → 回写 `qrcode_url`），后端可在后续接力补 server-side 兜底。

## 七、文件清单（新增/修改）

**新增**：

```
pengcheng-core/pengcheng-crm/pom.xml
pengcheng-core/pengcheng-crm/src/main/java/com/pengcheng/crm/package-info.java
pengcheng-core/pengcheng-crm/src/main/java/com/pengcheng/crm/lead/{entity,mapper,service,controller,dto}/*.java
pengcheng-core/pengcheng-crm/src/main/java/com/pengcheng/crm/customfield/{entity,mapper,service,controller}/*.java
pengcheng-core/pengcheng-crm/src/main/java/com/pengcheng/crm/visitmedia/{entity,mapper,service,controller,dto}/*.java
pengcheng-core/pengcheng-crm/src/main/java/com/pengcheng/crm/tag/{entity,mapper,service,controller}/*.java
pengcheng-core/pengcheng-crm/src/main/java/com/pengcheng/crm/ext/{entity,mapper,service}/*.java
pengcheng-core/pengcheng-crm/src/main/java/com/pengcheng/crm/importexport/{dto,service,controller}/*.java
pengcheng-core/pengcheng-crm/src/test/java/com/pengcheng/crm/**/*.java

pengcheng-starter/src/main/resources/db/migration/V43__crm_lead.sql
pengcheng-starter/src/main/resources/db/migration/V44__crm_custom_field.sql
pengcheng-starter/src/main/resources/db/migration/V45__customer_visit_media.sql
pengcheng-starter/src/main/resources/db/migration/V46__customer_tag.sql
pengcheng-starter/src/main/resources/db/migration/V47__customer_realty_ext.sql

pengcheng-ui/src/api/leadApi.ts
pengcheng-ui/src/api/customField.ts
pengcheng-ui/src/api/crmTag.ts
pengcheng-ui/src/api/crmImport.ts
pengcheng-ui/src/views/crm/lead/{index,assign,detail,convert}.vue
pengcheng-ui/src/views/crm/custom-field/{index,DynamicFieldRenderer}.vue
pengcheng-ui/src/views/crm/tag/index.vue
pengcheng-ui/src/views/crm/import-export/index.vue

pengcheng-uniapp/pages/lead/{list,detail,assign,convert}.vue
pengcheng-uniapp/pages/customer/follow-media.vue

docs/V4-MVP-D3-DELIVERY.md
```

**修改**（最小必要）：

```
pom.xml                                    -- dependencyManagement 增加 pengcheng-crm
pengcheng-core/pom.xml                     -- modules 增加 pengcheng-crm
```

## 八、后续接力建议

1. **uniapp 路由注册**：将 `pages/lead/*` 与 `pages/customer/follow-media.vue` 加入 `pages.json`（任务红线允许接力 Agent 处理）。
2. **Web 路由注册**：将 `views/crm/*` 接入 `pengcheng-ui/src/router`。两者均要绑定 RBAC 菜单。
3. **二维码服务端兜底**：用 `zxing` 或 `BarcodeGenerator` 生成 QR PNG 上传 MinIO。
4. **realty facade 接 ext 双写**：待 V3.2 WP-S5-A 错误码完善后，在 facade 侧把 `customer.visit_*` / `alliance_id` / `deal_probability` 等同时写一份到 `customer_realty_ext`。
5. **EAV 热字段冗余**：客户列表前端高频筛选项进入 `customer` 主表冗余列（PRD 风险表已注明）。
6. **opportunity 商机独立**：按 PRD 放在 Phase 5，本次未实现。
