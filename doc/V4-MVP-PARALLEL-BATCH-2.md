# V4.0 MVP — 第二批五路并行协调（D1～D5）

> 版本：rev.1（2026-04-26）
> 维护人：Claude（worktree `claude/recursing-spence-e31bd4`）
> 上游基线：HEAD `60542ae`（origin/main）
> 配套文档：[`PRD-V4.0-新产品需求文档.md`](./PRD-V4.0-新产品需求文档.md) §四、[`V4-MVP-CLAUDE-COORDINATION.md`](./V4-MVP-CLAUDE-COORDINATION.md)

## 1. 背景

V3.2 Sprint 5-7 在 `claude/friendly-wilbur-213d88` 分支并行推进；本批次直接从 `main` 派发 5 路 worktree-isolated agent，对 V4.0 MVP 五大闭环全部启动。每路严格按"文件红线"工作，提交后由协调人统一合并。

## 2. 五路任务划分

| # | 闭环 | 工作量 | Owner（worktree） |
|---|------|--------|------------------|
| D1 | 闭环① 账户与组织（企业注册 + 邀请 + 数据权限扩展 + 设备管理） | ~16.5 人日 | agent-D1 |
| D2 | 闭环② 日常办公 OA（班次/补卡 + 4 类审批模板 + 轻量串行审批引擎 + 配置 UI） | ~22 人日 | agent-D2 |
| D3 | 闭环③ 客户管理 CRM（Lead + 自定义字段 EAV + 多媒体跟进 + Excel + 行业表迁移） | ~30.5 人日 | agent-D3 |
| D4 | 闭环④ AI Copilot（统一组件库 + 智能提醒 + 语音 + Tool Call 业务动作） | ~28 人日 | agent-D4 |
| D5 | 闭环⑤ 移动办公（APP 打包流水线 + 原生插件 + 三通道推送统一调度 + 离线缓存） | ~29 人日 | agent-D5 |

## 3. 文件红线（零重叠）

| Owner | 后端可写区 | 前端 Web 可写区 | uniapp 可写区 | SQL Migration |
|-------|-----------|----------------|---------------|---------------|
| **D1** | `pengcheng-core/pengcheng-system/`（新增 `tenant/`、`invite/`、`device/` 子包）；`pengcheng-core/pengcheng-auth/`（注册路径） | `pengcheng-ui/src/views/register/`（新建）；`pengcheng-ui/src/views/org/invite/`（新建）；`pengcheng-ui/src/views/system/role-data-scope/`（新建）；`pengcheng-ui/src/views/system/user/device/`（新建）；`pengcheng-ui/src/api/tenant.ts`、`invite.ts`、`device.ts` | `pages/login/register-tenant.vue`（新增）；`pages/login/invite-accept.vue`（新增） | `V10__tenant_invite.sql`、`V11__user_login_device.sql` |
| **D2** | 新建模块 `pengcheng-core/pengcheng-oa/`（`shift/`、`correction/`、`approval-template/`、`approval-flow/`） | `pengcheng-ui/src/views/oa/shift/`、`oa/correction/`、`oa/approval-template/`、`oa/approval-flow/`；`pengcheng-ui/src/api/oa*.ts` | `pages/oa/`（新建）下子页面；`pages/approval/cards/`（新建）；现有 `pages/approval/`、`pages/apply/` 仅可只读引用其新组件 | `V12__attendance_shift.sql`、`V13__attendance_correction.sql`、`V14__approval_flow.sql` |
| **D3** | 新建模块 `pengcheng-core/pengcheng-crm/`（`lead/`、`custom-field/`、`visit-media/`、`tag/`、`ext/`）；同时在 `pengcheng-core/pengcheng-realty/` 内**仅做迁移脚本不动业务逻辑**（拆出 `customer_realty_ext` 字段映射） | `pengcheng-ui/src/views/crm/`（新建：`lead/`、`custom-field/`、`tag/`、`import-export/`）；`pengcheng-ui/src/api/crm*.ts`、`leadApi.ts`、`customField.ts`、`crmTag.ts` | `pages/lead/`（新建）；`pages/customer/follow-media.vue`（新增） | `V15__crm_lead.sql`、`V16__custom_field.sql`、`V17__customer_visit_media.sql`、`V18__customer_tag.sql`、`V19__customer_realty_ext.sql` |
| **D4** | `pengcheng-core/pengcheng-ai/`（仅在 `reminder/`、`copilot-action/`、`asr/` 子包新增；不动 `orchestration/`、`rag/`、`mcp/` 现有文件） | **新建** `pengcheng-ui/src/components/copilot/`；`pengcheng-ui/src/views/ai/copilot-config/`（新建）；`pengcheng-ui/src/api/aiCopilot.ts`、`aiReminder.ts`；在 `App.vue`、`main.ts` 注册全局 FAB | **新建** `pengcheng-uniapp/components/copilot/`；`pages/ai/copilot.vue`（新建）；现有 `pages/ai/` 老页面仅可补 router 跳转，不重写 | `V20__ai_reminder_rule.sql`、`V21__ai_copilot_action_log.sql` |
| **D5** | `pengcheng-core/pengcheng-message/`（新增 `channel-dispatch/`、`subscribe-msg/` 子包）；`pengcheng-infra/pengcheng-push/`（新增 `unified-dispatcher/`） | `pengcheng-ui/src/views/system/push-channel/`（新建）；`pengcheng-ui/src/api/pushChannel.ts` | `pengcheng-uniapp/scripts/`（新增打包脚本）；`pengcheng-uniapp/components/native/`（新建）；`pengcheng-uniapp/utils/offline.js`（新建）；`pengcheng-uniapp/utils/native-bridge.js`（新建）；`manifest.json` 配置；**不修改现有 pages/ 文件**，仅暴露组件供他批引用 | `V22__push_channel_log.sql`、`V23__subscribe_msg_template.sql` |

## 4. 共用文件（任何 agent 都不得修改）

- `doc/PRD-V4.0-新产品需求文档.md`
- `doc/DEV-PLAN-V3.2.md`
- `doc/V4-MVP-CLAUDE-COORDINATION.md`
- `doc/V4-MVP-PARALLEL-BATCH-2.md`（本文件，仅协调人维护）
- `pengcheng-common/src/main/java/com/pengcheng/common/feature/FeatureFlags.java`
- `pengcheng-common/src/main/java/com/pengcheng/common/annotation/DataScope.java`
- `pengcheng-infra/pengcheng-db/src/main/java/com/pengcheng/db/interceptor/DataPermissionInterceptor.java`
- `pengcheng-uniapp/manifest.json`（仅 D5 可改 `app-plus` 节点；其他 agent 仅读）
- `sql/V0__*.sql ~ V9__*.sql`（已落历史 migration 不得修改）

## 5. 协议规则

1. **commit 前缀**：`feat(scope-d{N}): ...` 中文描述；scope 用闭环短名（tenant/oa/crm/copilot/mobile）。
2. **migration 号区段**：D1 用 V10-V11；D2 用 V12-V14；D3 用 V15-V19；D4 用 V20-V21；D5 用 V22-V23。**不允许跨段占用**。
3. **DoD（完工口径）**：每路必须带 ① 后端单测（≥ 60% 行覆盖）；② 前端/uniapp 至少 1 个端到端用例（vitest/playwright/uni 自测脚本）；③ Flyway 迁移本地 `mvn -pl pengcheng-starter test` 不报错；④ README/PRD 不动，但在自身分支顶层新增 `docs/V4-MVP-D{N}-DELIVERY.md` 记录交付。
4. **失败处理**：若 agent 中途遇到红线模糊地带，停下并把"建议红线调整"写入交付文档，由协调人决策。
5. **完工后报告**：每个 agent 完工时输出 ①分支名 ②commit 列表 ③改动文件清单 ④运行通过的测试命令 ⑤后续接力建议。

## 6. 进度日志

| 日期 | 事件 |
|------|------|
| 2026-04-26 | 协调人创建本文件，并发启动 D1～D5 五个 worktree-isolated agent。 |
