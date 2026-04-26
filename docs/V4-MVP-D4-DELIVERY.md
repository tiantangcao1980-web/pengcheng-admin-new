# V4.0 MVP — D4 闭环④「AI 智能助手」交付清单

> 分支：`claude/recursing-spence-e31bd4`
> 角色：D4 — Copilot / 智能提醒 / Tool Call / ASR / 成本路由
> 红线遵循：仅在 ai 模块 `reminder/` `copilot/action/` `asr/` `cost/` 子包内工作，未触碰
> `orchestration/` `rag/` `mcp/` 与 `AiChatController` 等现有文件。

---

## 1. 交付概要

| # | 子项 | 状态 | 说明 |
|---|------|------|------|
| 1 | 统一 Copilot 前端组件库（Web） | ✔ | `pengcheng-ui/src/components/copilot/`：FAB / Drawer / Bubble / StreamRenderer / Store |
| 2 | 统一 Copilot 前端组件库（uniapp） | ✔ | `pengcheng-uniapp/components/copilot/` + `pages/ai/copilot.vue` 新页面 |
| 3 | 全局上下文注入（用户/页面/可执行动作） | ✔ | 通过 `CopilotContext` DTO 随 `chat`/`actions/propose` 一同发送 |
| 4 | 对话驱动业务动作（Tool Call） | ✔ | `CopilotActionService` 两阶段：propose 生成 confirmToken → confirm 执行；3 个动作编码 `FOLLOW_UP_CREATE / TODO_CREATE / APPROVAL_SUBMIT` |
| 5 | 智能提醒规则表 + 调度 | ✔ | `ai_reminder_rule` + `ReminderRuleEngine` + `ReminderScheduler`（@Scheduled，仅在 ai 模块）|
| 6 | 智能推送触发 | ✔ | `ChannelPushPortAdapter` → 现有 `ChannelPushService.broadcast` |
| 7 | 语音输入 ASR | ✔ | `DashScopeAsrService`（mock 桩 + TODO 真实接入注释）|
| 8 | APP `pages/ai` 30% 补齐 | ✔ | 新建 `pages/ai/copilot.vue`（含 mic 录音、确认按钮、流式 polling）|
| 9 | AI 成本控制（分级路由 + 配额） | ✔ | `CostRouterService` 6 路决策 + `InMemoryAiQuotaStore` |
| 10 | Copilot 操作日志表 | ✔ | `ai_copilot_action_log`（V44） |

---

## 2. Flyway 迁移

> 任务原文档说"仅可使用 V20、V21"，但项目主分支已使用至 V42。
> 本次按版本顺延使用 **V43 / V44**（语义对齐：1 张提醒规则表 + 1 张操作日志表）。
> 详见红线问题 §6。

| 文件 | 内容 |
|------|------|
| `pengcheng-starter/src/main/resources/db/migration/V43__ai_reminder_rule.sql` | 表结构 + 3 条内置规则种子（DAILY_FOLLOWUP / APPROVAL_PENDING / POOL_RECYCLE_PRE） |
| `pengcheng-starter/src/main/resources/db/migration/V44__ai_copilot_action_log.sql` | 操作日志表（含 confirm_token、status 枚举 PENDING/CONFIRMED/EXECUTED/CANCELLED/FAILED） |

---

## 3. 后端文件清单

```
pengcheng-core/pengcheng-ai/src/main/java/com/pengcheng/ai/
├── reminder/
│   ├── entity/AiReminderRule.java
│   ├── mapper/AiReminderRuleMapper.java
│   ├── ReminderTarget.java
│   ├── ReminderTargetCollector.java
│   ├── NoopReminderTargetCollector.java
│   ├── ReminderPushPort.java
│   ├── ChannelPushPortAdapter.java       ← 委托给现有 ChannelPushService
│   ├── ReminderRuleEngine.java           ← 三种规则类型的触发判断 + 模板渲染
│   ├── ReminderScheduler.java            ← @Scheduled cron=每分钟
│   └── ReminderSchedulingConfig.java     ← @EnableScheduling（仅本模块）
├── copilot/action/
│   ├── entity/AiCopilotActionLog.java
│   ├── mapper/AiCopilotActionLogMapper.java
│   ├── CopilotActionRequest.java
│   ├── CopilotActionProposal.java
│   ├── CopilotActionExecutor.java        ← 业务侧实现的 SPI
│   └── CopilotActionService.java         ← 两阶段提交，含 TTL & 幂等
├── asr/
│   ├── AsrRequest.java
│   ├── AsrResponse.java
│   ├── AsrService.java
│   └── DashScopeAsrService.java          ← MVP 阶段为 mock；TODO 标注真实接入
└── cost/
    ├── ModelTier.java                    ← SMALL / MEDIUM / LARGE
    ├── CostRouteRequest.java
    ├── CostRouteDecision.java
    ├── AiQuotaStore.java
    ├── InMemoryAiQuotaStore.java         ← 默认 1M token / 天 / 租户，按日重置
    └── CostRouterService.java            ← 6 条 first-match-wins 规则
```

> 注意：本模块**未提供** Controller。前端契约路径已在 `aiCopilot.ts` /
> `aiReminder.ts` / `copilotApi.js` 中约定，对应 Controller 由 admin-api 模块在
> 联调 sprint 落地（红线：不动 AiChatController），调用本模块 Service Bean 即可。

---

## 4. 前端文件清单

### Web（pengcheng-ui）

```
src/components/copilot/
├── CopilotStore.ts                  ← reactive 单例，pushMessage / openCopilot
├── StreamRenderer.vue               ← marked + 打字光标
├── MessageBubble.vue                ← 用户/助手气泡 + 工具确认面板
├── CopilotDrawer.vue                ← 抽屉 + SSE 流式 + Tool Call 二次确认
├── CopilotFab.vue                   ← 全局右下角悬浮按钮
├── index.ts
└── __tests__/
    ├── setup.ts
    └── CopilotDrawer.spec.ts        ← 流式渲染 + 确认面板单测
src/views/ai/copilot-config/index.vue ← 提醒规则配置中心
src/api/aiCopilot.ts                  ← chat / stream / propose / confirm / cancel / transcribe
src/api/aiReminder.ts                 ← list / update / fire
```

App.vue / package.json 改动均**最小**：仅 1 行 `<CopilotFab />` 注入 + scripts 加 `test`/`test:watch` + 3 个 devDeps（vitest / @vue/test-utils / jsdom）。

### uniapp（pengcheng-uniapp）

```
components/copilot/
├── copilot-fab.vue
├── copilot-bubble.vue
└── copilotApi.js                    ← request 封装；pollChat 长轮询代替 SSE
pages/ai/copilot.vue                  ← 新页面（不动既有 chat.vue）
pages.json                            ← 仅追加一条路由（最小 diff）
```

---

## 5. 测试与覆盖

### 5.1 后端单测（≥ 60% 行覆盖率目标）

| 测试类 | 覆盖断言 |
|--------|---------|
| `ReminderRuleEngineTest` | DAILY 9:00 触发、同日防抖、cron 非工作时段不触发、THRESHOLD/PRE_EXPIRE 任意时刻 true、模板渲染 ${count}/${threshold}、fire() 调用 pushPort + 更新 lastFiredAt、空目标兜底、非法 cron 不抛异常、disabled 永不触发 |
| `ChannelPushPortAdapterTest` | 委托至 `ChannelPushService.broadcast`、type 为 `ai_reminder:<code>`、空 content 不推送、推送异常吞掉返回 false |
| `CopilotActionServiceTest` | propose 生成 PENDING + token、confirmToken 错误抛异常、TTL 过期 → CANCELLED、confirm 正常执行 → EXECUTED、幂等、cancel 关闭、token 不匹配的 cancel 忽略、stub fallback |
| `CostRouterServiceTest` | 6 条规则路径全覆盖（SMALL / MEDIUM by tokens / MEDIUM by history / LARGE by tokens / LARGE multi-modal / LARGE high-quality）、按 token 数升级 SMALL→MEDIUM→LARGE、配额耗尽强制 SMALL、modelName fallback |
| `InMemoryAiQuotaStoreTest` | 默认配额、扣减、超额负值、null tenant、非正数扣减忽略 |
| `DashScopeAsrServiceTest` | apiKey 缺失 → mock provider + audio 文件名回显、缺 audioUrl 抛 IllegalArgument |

执行命令：
```bash
mvn -pl pengcheng-core/pengcheng-ai -am test -DskipITs
```

### 5.2 Web 单测

| 文件 | 断言 |
|------|------|
| `src/components/copilot/__tests__/CopilotDrawer.spec.ts` | StreamRenderer 渲染 Markdown bold、streaming=true 显示打字光标、prop 变化追加内容、用户/助手气泡渲染、pendingAction 存在时显示确认面板并在点击时 emit `confirm` |

执行命令：
```bash
cd pengcheng-ui && npm install && npm run test
```

> 首次执行需要安装 vitest / @vue/test-utils / jsdom（已写入 package.json devDependencies）。

---

## 6. 红线问题与决策

| 红线条目 | 处理 |
|----------|------|
| 后端仅在 ai 模块 4 个新子包工作 | ✔ — `reminder/` `copilot/action/` `asr/` `cost/` 全部为新建 |
| 不动 `orchestration/` `rag/` `mcp/` `AiChatController` | ✔ — 仅 import-only 引用 ChannelPushService（system 模块） |
| 仅在 ai 模块新增 Job Bean | ✔ — `ReminderScheduler` 用 `@Scheduled` 不动 pengcheng-job/ Quartz |
| Web 仅新建 `components/copilot/` 等 | ✔ — App.vue 仅 1 行注入 FAB；package.json 仅增 scripts/devDeps |
| uniapp 不重写既有 ai 页面 | ✔ — 新建 `pages/ai/copilot.vue`，pages.json 仅追加路由 |
| SQL Migration 限制 | ⚠ **任务文档中"仅可使用 V20、V21"与现有 V42 冲突；按 V43 / V44 顺延以避免 Flyway 校验失败**。语义保留：1 表 ai_reminder_rule + 1 表 ai_copilot_action_log。 |
| AI 成本控制不修改 RouterService / OrchestratorService | ✔ — `CostRouterService` 仅做 advisory，调用侧（admin-api 控制器）按需调用 |
| ASR 留 TODO + DashScope 配置说明 | ✔ — `DashScopeAsrService` 顶部 javadoc + 内联 TODO |
| 不使用 mvn install | ✔ — 测试命令统一 `mvn -pl pengcheng-core/pengcheng-ai -am test -DskipITs` |

---

## 7. 接力建议（≤ 250 词）

1. **admin-api 控制器层**：建议在 `pengcheng-api/pengcheng-admin-api/src/main/java/com/pengcheng/admin/controller/ai/` 新增
   `AiCopilotController`（`/admin/ai/copilot/...`）和 `AiReminderRuleController`（`/admin/ai/reminder/...`），把 `CopilotActionService`/`ReminderRuleEngine` 暴露给前端。**不要**改 `AiChatController`。
2. **流式响应**：当前前端已按 SSE 协议 `data:` 解析；admin-api 落地时，可复用 `AgentOrchestratorService.streamOrchestrate(...)`，把 chunk 包装为 `data:<json>\n\n`。后端建议在最后一帧附 `[SUGGEST]:{actionCode,payload,summary}` 触发前端动作提议。
3. **业务侧 Bean 注入**：
   - `ReminderTargetCollector`：分别由 pengcheng-realty 提供（DAILY_FOLLOWUP / POOL_RECYCLE_PRE）和 pengcheng-system 提供（APPROVAL_PENDING）；
   - `CopilotActionExecutor`：3 个 MVP 动作执行器（FOLLOW_UP_CREATE 走 customerVisit、TODO_CREATE 走 todo、APPROVAL_SUBMIT 走 audit_flow）。未注入时本模块自动走 stub，不阻塞 LLM。
4. **AI 配额接 Redis**：`InMemoryAiQuotaStore` 适合单实例 MVP；多实例换成 RedisQuotaStore（HINCRBY + EXPIRE 86400）即可，接口 `AiQuotaStore` 已抽好。
5. **DashScope ASR**：`DashScopeAsrService.transcribe()` 顶部 TODO 已写明真实接入步骤；启用前需配置 `spring.ai.dashscope.api-key`。
6. **uniapp 流式**：当前是 polling 兜底；APP 端如需真流式，建议在 plus 环境下用 `plus.net.XMLHttpRequest` 配合 `responseType=stream`。
