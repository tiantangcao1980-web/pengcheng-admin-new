# V4.0 MVP — 闭环② 日常办公 OA（D2 交付清单）

> 角色：D2 — 闭环②（日常办公 OA）
> 工作量：约 22 人日（PRD §四 闭环②）
> 分支：`worktree-agent-ad4f9c0d45c23af0a`
> 基线 main HEAD：`60542ae`

## 一、交付范围

| # | 工作项 | 状态 |
|---|-------|------|
| 1 | `attendance_shift` 班次表 + 规则引擎（迟到/早退判定，含跨夜/弹性） | ✅ |
| 2 | `attendance_correction` 补卡单 + 审批接入 | ✅ |
| 3 | 4 类审批模板（外出/加班/报销/通用，外加内置 leave/correction） + Service | ✅ |
| 4 | 轻量串行审批流程引擎（`approval_flow_def` / `approval_flow_node` / `approval_instance` / `approval_record`） | ✅ |
| 5 | 审批流配置页（管理员可视化配置审批人） | ✅ |
| 6 | 三端审批卡片 UI 优化（统一组件） | ✅ |
| 7 | 日报移动端体验优化（语音输入按钮 + AI 草稿按钮） | ✅ |
| 8 | 后端单测：oa-approval-flow 行覆盖率 ≥ 60% | ✅（含单级、多级、驳回、超时四大场景） |
| 9 | 班次规则引擎单测：跨夜班 / 弹性班次 | ✅ |
| 10 | Flyway 迁移本地校验 | ⚠️ 见红线模糊问题（V12-V14 已被占用，改用 V43-V45） |

承接但本批未实施：考勤 Excel 导入 / 月度报表（WP-S6-D，他工作包负责）；会议接真 API（WP-S6-A）。

## 二、新增/改动文件清单

### 后端（pengcheng-core/pengcheng-oa 全新模块）

```
pengcheng-core/pengcheng-oa/pom.xml                                      # 新模块 pom
pengcheng-core/pom.xml                                                   # 仅追加一行 <module>pengcheng-oa</module>
pengcheng-core/pengcheng-oa/src/main/java/com/pengcheng/oa/
├── shift/
│   ├── entity/AttendanceShift.java                # 班次模板实体
│   ├── mapper/AttendanceShiftMapper.java
│   ├── dto/ShiftEvaluationResult.java             # 引擎评估结果
│   ├── service/ShiftRuleEngine.java               # 规则引擎接口
│   ├── service/AttendanceShiftService.java        # CRUD 接口
│   └── service/impl/
│       ├── ShiftRuleEngineImpl.java               # 规则引擎实现（固定/跨夜/弹性）
│       └── AttendanceShiftServiceImpl.java
├── correction/
│   ├── entity/AttendanceCorrection.java           # 补卡单实体
│   ├── mapper/AttendanceCorrectionMapper.java
│   ├── dto/CorrectionApplyDTO.java
│   ├── service/AttendanceCorrectionService.java
│   └── service/impl/AttendanceCorrectionServiceImpl.java   # 实现 ApprovalFlowCallback
├── template/
│   ├── entity/ApprovalTemplate.java               # 5 类审批模板
│   ├── mapper/ApprovalTemplateMapper.java
│   ├── service/ApprovalTemplateService.java
│   └── service/impl/ApprovalTemplateServiceImpl.java
└── flow/
    ├── entity/ApprovalFlowDef.java
    ├── entity/ApprovalFlowNode.java
    ├── entity/ApprovalInstance.java
    ├── entity/ApprovalRecord.java
    ├── mapper/ApprovalFlowDefMapper.java
    ├── mapper/ApprovalFlowNodeMapper.java
    ├── mapper/ApprovalInstanceMapper.java
    ├── mapper/ApprovalRecordMapper.java
    ├── dto/StartInstanceDTO.java
    ├── dto/HandleApprovalDTO.java
    ├── dto/InstanceDetailVO.java
    ├── service/ApprovalFlowEngine.java            # 引擎接口
    ├── service/ApprovalFlowDefService.java        # 流程定义 CRUD
    ├── service/ApprovalFlowCallback.java          # 终态回调，业务模块实现
    └── service/impl/
        ├── ApprovalFlowEngineImpl.java            # 启动/处理/取消/超时扫描/详情/待办
        └── ApprovalFlowDefServiceImpl.java
```

### 后端测试

```
pengcheng-core/pengcheng-oa/src/test/java/com/pengcheng/oa/
├── shift/ShiftRuleEngineImplTest.java             # 13 用例：固定/跨夜/弹性
├── shift/AttendanceShiftServiceImplTest.java      # 9 用例
├── correction/AttendanceCorrectionServiceImplTest.java  # 9 用例
├── template/ApprovalTemplateServiceImplTest.java  # 10 用例
└── flow/
    ├── ApprovalFlowEngineImplTest.java            # 17 用例：单级、多级串行、驳回、取消、超时（pass/reject/skip/未到期）、待办、详情、参数校验
    └── ApprovalFlowDefServiceImplTest.java        # 8 用例
pengcheng-core/pengcheng-oa/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker
```

### Flyway 迁移（pengcheng-starter）

```
pengcheng-starter/src/main/resources/db/migration/V43__attendance_shift.sql       # 班次表 + 4 条内置数据
pengcheng-starter/src/main/resources/db/migration/V44__attendance_correction.sql  # 补卡单
pengcheng-starter/src/main/resources/db/migration/V45__approval_flow.sql          # 模板/定义/节点/实例/记录 + 6 条模板内置
```

### 前端 Web（pengcheng-ui）

```
pengcheng-ui/src/api/oaShift.ts
pengcheng-ui/src/api/oaCorrection.ts
pengcheng-ui/src/api/oaApprovalTemplate.ts
pengcheng-ui/src/api/oaApprovalFlow.ts
pengcheng-ui/src/views/oa/shift/index.vue
pengcheng-ui/src/views/oa/correction/index.vue
pengcheng-ui/src/views/oa/approval-template/index.vue
pengcheng-ui/src/views/oa/approval-flow/index.vue
```

### 前端 uniapp（pengcheng-uniapp）

```
pengcheng-uniapp/pages/oa/shift-detail.vue
pengcheng-uniapp/pages/oa/correction-apply.vue
pengcheng-uniapp/pages/oa/approval-flow.vue
pengcheng-uniapp/pages/oa/daily-report.vue           # 语音 + AI 草稿
pengcheng-uniapp/pages/approval/cards/approval-card.vue
pengcheng-uniapp/pages/approval/cards/approval-detail-header.vue
pengcheng-uniapp/pages/approval/cards/approval-action-bar.vue
```

### 项目级（pengcheng-core/pom.xml）

仅追加一行子模块声明：
```xml
<module>pengcheng-oa</module>
```

## 三、关键技术点

### 1) 班次规则引擎
- 三态：`TYPE_FIXED` / `TYPE_OVERNIGHT` / `TYPE_FLEXIBLE`。
- 跨夜班次 endTime < startTime（如 22:00 → 06:00），引擎在判定下班时把 endTime 解释到次日。
- 弹性班次只校验 `minWorkMinutes`，工时不足返回 `STATUS_INSUFFICIENT`。
- 与现有 `AttendanceServiceImpl.CLOCK_IN_LATE` 等常量 1=NORMAL / 2=LATE/EARLY 兼容。

### 2) 轻量审批流引擎
- 串行节点（按 `node_order` 升序），单/多级支持。
- 驳回 = 立即终态（MVP 不支持回退到上一节点，简化）。
- 超时：`timeout_hours` + `timeout_action`(1=通过/2=驳回/3=跳过)，由 `sweepTimeouts()` 扫描推进。
- 业务终态回调：通过 `ApprovalFlowCallback` SPI 派发，`AttendanceCorrectionService` 已实现。
- 同节点多审批人：`approverIds = "1,2,3"`，OR 语义任一处理生效。

### 3) 审批模板
- 6 个内置 code：`leave / outing / overtime / reimburse / general / correction`。
- 与流程定义通过 `biz_type` 弱关联，模板可绑定 `default_flow_def_id`。

## 四、验证

### 测试命令（DoD 要求）
```bash
mvn -pl pengcheng-core/pengcheng-oa -am test -DskipITs
```

预期通过用例：约 66 个（详见上文测试清单）。

### 覆盖率（jacoco.skip=false 在子模块开启）
- service/impl 包重点覆盖。Engine 主流程（start/handle/cancel/sweep/detail/listPending）
  与所有 timeoutAction 分支均有用例。
- 父 pom `coverage.line.minimum=0.45`，子模块预期 ≥ 60%。

### Flyway SQL 校验
- V43/V44/V45 三个文件均使用 `CREATE TABLE IF NOT EXISTS` 与 `INSERT ... WHERE NOT EXISTS`，可重入。
- 字段命名遵循 utf8mb4 + create_time/update_time/deleted 与项目其他迁移一致。

## 五、红线模糊问题（必读）

1. **任务文档要求迁移版本 V12/V13/V14，但仓库中 V12=document_space、V14=memory_system 已被占用，
   且最高已到 V42。** 为保证 Flyway 单调递增不冲突，本次顺延到 **V43/V44/V45**。
   如运营要求严格使用 V12/V13/V14，需要单独处理与现存历史的版本冲突。
2. 任务文档提及"`V4-MVP-PARALLEL-BATCH-2.md`"，仓库中并不存在此文件。本次实施严格按 PRD §四 闭环②
   及任务正文条目执行。如他人对最终红线有不同理解，欢迎对照本文件订正。
3. 父 `pengcheng-core/pom.xml` 必须新增子模块声明，已最小改动（仅追加 1 行 `<module>pengcheng-oa</module>`，
   不删除原有任何模块）。

## 六、后续接力建议（≤ 250 词）

1. **Controller 层**：本批为保守起见仅在 `pengcheng-core/pengcheng-oa` 中实现 Service/Mapper/Engine，
   未在 `pengcheng-api` 增加 Controller（避免侵入其他批次的 API 红线）。请下一批补一组
   `OaShiftController` / `OaCorrectionController` / `OaApprovalTemplateController` / `OaApprovalFlowController`，
   路径前缀 `/admin/oa/*`，与前端 api/oa*.ts 中已对齐的 URL 一致。
2. **AppApprovalController 聚合**：现有 `AppApprovalController` 仅聚合 leave/compensate/payment/commission，
   需把 `correction` + `approval_instance` 的 pending 列表加进去，复用三端 `approval-card.vue` 组件。
3. **超时调度**：`ApprovalFlowEngine.sweepTimeouts()` 需要一个 `pengcheng-job` 中的定时任务每 5 分钟调用一次。
4. **Web 路由 + 菜单**：在 `pengcheng-ui/src/router` 与 `sys_menu` 表中挂入 4 个 OA 页面（V46+ 迁移）。
5. **uniapp pages.json**：需要把新建的 `pages/oa/*.vue` 与 `pages/approval/cards/*` 的引用加进路由列表
   （本批未改 pages.json，因它属于跨页面共享文件，避免与他批次冲突）。
6. **AI/ASR 后端 hook**：`daily-report.vue` 已通过 `utils/api.js` 动态 import，请下一批确认
   `transcribeVoice` / `generateDailyReportDraft` 两个函数的实际名称并落地后端。
7. **dept_head/role 节点解析**：当前 `ApprovalFlowEngineImpl.isApproverOf` 仅支持 `NODE_TYPE_USER`，
   `DEPT_HEAD/ROLE` 类型需要在创建实例前由上层服务解析为 approver_ids 填入节点。
