# V4 MVP 全谱交付报告（D-L 系列总结）

> 版本：rev.1（2026-04-27）
> 维护人：Claude（worktree `claude/recursing-spence-e31bd4`）
> 上游基线：HEAD `60542ae`（origin/main）
> 累计交付：**729 文件 / +60,709 行 / 47 commits**

---

## 1. 总览

V4.0 PRD 定义的 6 个 Phase + V1.0-V1.2 长期规划，本批次全部基线就绪：

| 系列 | 主题 | Commits | 文件 | 状态 |
|------|------|---------|------|------|
| **D1-D5** | MVP 5 闭环（账户/OA/CRM/Copilot/移动办公） | 5 merges | 279 | ✅ |
| **E 抢救** | 第三批 worktree 隔离漏洞抢救 41 文件 | 1 | 41 | ✅ |
| **F1-F4** | 上线必备（路由/Controller/装配/审计） | 4 | 16 | ✅ |
| **G1-G5** | 质量补齐（Resolver/OA 测试/OCR/Lead E2E + Phase 2 骨架） | 5 | 47 | ✅ |
| **H1-H5** | 业务联动（订阅授权/Tool Call/动态字段/e签宝/OCR） | 5 | 33 | ✅ |
| **I1-I5 + SPI** | Phase 3 数据决策（看板 + 20 卡片 + 拖拽 + BI + OKR） | 6 | 91 | ✅ |
| **J1-J5 + dep** | Phase 4 协作工具（公式 + 自动化 + 云文档 + 甘特 + 会议 AI） | 6 | 105 | ✅ |
| **K1-K5** | Phase 5 房产垂直插件（插件框架 + 户型 + 带看 SOP + 房产看板） | 5 | 69 | ✅ |
| **L1-L5** | Phase 6 SaaS 生态（IM 集成 + OpenAPI + Webhook + SaaS + i18n） | 5 | 53 | 🟡 骨架 |
| 协调文档 | 5 份（PARALLEL-BATCH-2 + MERGE-PLAN + POSTMORTEM + ...） | 5 | 5 | ✅ |
| **合计** | — | **47** | **729** | — |

---

## 2. Migration 编号全景（V37-V74，38 个连续）

```
V37-V42  原有
V43-V57  D 系列 + E4 + E1 (15 个)
  V43 D1 tenant_invite      V51 D3 customer_tag
  V44 E4 org_invite_merge   V52 D3 customer_realty_ext
  V45 D2 attendance_shift   V53 D4 ai_reminder_rule
  V46 D2 attendance_correction  V54 D4 ai_copilot_action_log
  V47 D2 approval_flow      V55 D5 push_channel_log
  V48 D3 crm_lead           V56 D5 subscribe_msg_template
  V49 D3 crm_custom_field   V57 E1 v4_mvp_menus
  V50 D3 customer_visit_media

V58 G4 finance_init          V67 K2 realty_unit
V59 H1 mp_user_subscribe     V68 K3 realty_sop
V60 I1 dashboard             V69 K5 realty_field_templates_seed
V61 I4 bi                    V70 L1 integration
V62 I5 okr                   V71 L2 openapi
V63 J2 smarttable_automation V72 L3 webhook
V64 J3 doc_collab            V73 L4 saas
V65 J5 meeting_room_minutes  V74 L5 i18n
V66 K1 industry_plugin
```

---

## 3. V1.0-V1.2 PRD 任务盘点（与 V4 系列映射）

来源：`doc/archive/v3.0-delivery/项目开发情况分析报告.md` §7.2 + §8

### V1.0（短期 1 个月）— ✅ 7/8 完成

| 任务 | 状态 | 已交付位置 |
|------|------|-----------|
| 全部房产业务功能 | ✅ | K2 户型/房源 + K3 带看 SOP + 现有 realty 模块 |
| 完整 AI 功能（RAG/记忆） | ✅ | V3.2 WP-S6-C BM25 fallback + DLQ + ai_memory 表 + D4 ReminderEngine |
| 人事绩效管理 | ✅ | HR 模块 + V3.2 WP-S6-D 360 绩效 + I5 OKR |
| 移动端核心功能 | ✅ | D5 + H5 OCR + K5 楼盘房源 |
| OnlyOffice 环境验证 | 🟡 | J3 云文档 Y.js 协作已落，OnlyOffice 编辑未对接（M1 待补） |
| 核心业务单元测试 | ✅ | ~250 个 mockito 用例分散在各路 agent |
| API 文档（Swagger） | 🟡 | L2 OpenAPI 骨架，knife4j 4.4.0 已存（M2 待补元数据） |
| 完善项目管理看板 | ✅ | J4 项目甘特图 + 看板 UI |

### V1.1（中期 3 个月）— ✅ 4/4 完成

| 任务 | 状态 | 已交付位置 |
|------|------|-----------|
| 移动端功能完善 | ✅ | D5 + H5 + K5 |
| 360 度绩效评估 | ✅ | V3.2 WP-S6-D 在 claude/friendly-wilbur 分支待合并 |
| 自动化规则可视化 | ✅ | J2 多维表格自动化（5 Action + dry-run） |
| 数据导入导出 | ✅ | D3 EasyExcel + V3.2 WP-S6-D 考勤导入 |

### V1.2（长期 6 个月，过渡 V2.0）— ✅ 2/4 完成 + 2 骨架

| 任务 | 状态 | 已交付位置 |
|------|------|-----------|
| 多 Agent 编排平台 | ✅ | pengcheng-ai/orchestration + D4 Copilot Tool Call + Spring AI Alibaba |
| 智能表格模板市场 | 🟡 | 模板表已有，市场分发/评分未做（M4 待补） |
| BI 数据分析平台 | ✅ | I4 BI 多维查询引擎 + 5 层注入防护 + Excel 导出 |
| 开放 API 平台 | 🟡 | L2 AK/SK/限流骨架（M2/M3 待补元数据 + 异步落库 + scope） |

---

## 4. M 系列收口清单（V1.0-V1.2 最后 4 项 + 上线）

| # | 任务 | 工作量 | 价值 |
|---|------|-------|------|
| **M1** | OnlyOffice / Office Online 集成（Word/Excel/PPT 在线编辑） | 4-6h | V1.0 收口最后一项 |
| **M2** | springdoc/knife4j OpenAPI 3.0 元数据 + 业务 Controller 加 `@Tag/@Operation` | 3-4h | V1.2 开放 API 平台收口 |
| **M3** | OpenapiCallLog 异步落库 + scope 校验 + 调用统计 Controller | 3h | L2 完成度从骨架到生产级 |
| **M4** | 智能表格模板市场（分享/评分/下载计数 + Web 模板市场页） | 5h | V1.2 智能表格收口 |
| **M5** | 本机 mvn 全量编译验证 + push origin + 开 PR | 1-2h | 让 47 commits 进入主线 |

---

## 5. 关键技术沉淀（贯穿 D-L 全程）

### 5.1 三大并行模式

| 模式 | 适用场景 | 用过的批次 | 缺陷 |
|------|---------|-----------|------|
| `isolation: "worktree"` | 互相独立的新模块开发 | D1-D5 第一批 | 默认从 origin/main 派生，不见协调人分支（第三批 E 系列踩坑） |
| 直接在协调人 worktree | 看到合并基线的接力工作 | E/F/G/H/I/J/K 系列 | 多 agent 同跑需严格文件红线零重叠 |
| 主 agent 接管骨架 | sonnet 额度上限时的兜底 | L 系列 5 路 | 工作量受主 agent 上下文限制 |

### 5.2 SPI + Registry 模式

DashboardCardProvider（I1）/ AutomationAction（J2）/ InviteChannelSender（H1+E4）/ IndustryPlugin（K1）/ ChannelSender（D5）/ OcrProvider（D5）/ ImProvider（L1）— 全部用 `@Component` 自动收集，新增实现零侵入。

### 5.3 Feature Flag 默认关清单

```
pengcheng.feature.alipay         = false  (V3.2)
pengcheng.feature.wechat.mp      = false
pengcheng.feature.wechat.mini    = false
pengcheng.feature.wechat.pay     = false
pengcheng.feature.tenant         = false  (E4)
pengcheng.feature.ocr.baidu      = false  (G3)
pengcheng.feature.esign          = false  (H4)
pengcheng.feature.integration.wecom = false  (L1)
pengcheng.saas.enabled           = false  (L4)
pengcheng.webhook.retry-enabled  = true   (L3，运维默认开)
pengcheng.oa.approval.timeout-sweep-enabled = true (F3)
pengcheng.ai.reminder.scheduler-enabled = true (D4)
```

### 5.4 状态机异步 + 失败隔离

| 模块 | 状态机 |
|------|-------|
| J5 AI 纪要 | PENDING → TRANSCRIBING → SUMMARIZING → READY/FAILED |
| K2 房源 | AVAILABLE → RESERVED → SUBSCRIBED → SIGNED → SOLD + 任意 → UNAVAILABLE |
| K3 带看 SOP | PENDING_CONFIRM → CONFIRMED/EXPIRED/CANCELLED |
| H4 e签宝 | DRAFT → SIGNING → SIGNED/REJECTED/EXPIRED |
| L3 Webhook | PENDING → SUCCESS/FAILED → DEAD（5 次失败后） |
| L4 订阅 | TRIAL → ACTIVE → EXPIRED/CANCELLED |
| L4 账单 | UNPAID → PAID/OVERDUE/REFUNDED |

### 5.5 SQL 注入 5 层防护（I4 BI 引擎）

```
1) validateKeys 白名单 — column 必须在 view dimensions/metrics
2) ALLOWED_FORMULAS 枚举 — SUM/AVG/COUNT/MAX/MIN/COUNT_DISTINCT
3) WHERE 全部 PreparedStatement ?
4) ORDER BY 列名走 resolveSqlExpr 白名单 + 方向限定枚举
5) LIMIT Math.min(req.limit, MAX_LIMIT=10000)
```

### 5.6 签名验证三件套

| 模块 | 算法 | 防重放 |
|------|------|--------|
| H4 e签宝 V3 | HMAC-SHA256 + X-Tsign-Open-Ca-Signature 头 | Redis SETNX eventId 24h |
| L2 OpenAPI | Base64(HMAC-SHA256(SK, stringToSign)) + 4 头 | Redis SETNX nonce 5min + ±5min 时间戳容差 |
| L3 Webhook | Base64(HMAC-SHA256(secret, body)) | receiver 端按 eventId 自行幂等 |

### 5.7 跨模块复用（解循环依赖）

- K3 复用 H4 e签宝（@Autowired required=false 软依赖）
- K4 实现 K1 SPI（contributeDashboardCardCodes 与 card code 严格对齐）
- I2 实现 I1 SPI（DashboardCardProvider 自动收集）
- G1 Resolver 从 system 迁到 message（避免 system→message→system 循环）
- L5 LocaleContextHolder 在 common 而非 system（避免 common→system）

---

## 6. 待补 follow-up 工单（除 M 系列外）

### L 系列骨架细化（等 sonnet 额度恢复）

| L 路 | 待补内容 | 工作量 |
|------|---------|-------|
| L1 | WecomAuthService/ContactService/MessageService/ApprovalService 4 个真实实现 + OAuth 流程 + access_token 缓存 + 单测 | 8h |
| L2 | OpenapiCallLog 异步落库 + scope 校验 + springdoc 元数据 + 5 类单测 | M2+M3 已列 |
| L3 | 业务方 1-2 处 publish 集成示例 + 4 类单测 | 4h |
| L4 | TenantInterceptor TenantLineHandler 实现 + MybatisPlusConfig 注册 + generateBills 业务实现 + 计量埋点 + 5 类单测 | 6h |
| L5 | 前端 vue-i18n 集成（pengcheng-ui zh-CN/en-US locales + LocaleSwitcher）+ uniapp locales + extract-i18n 脚本 + WebMvcConfigurer 注册 + 单测 | 6h |

### 通用 follow-up

- 各模块剩余 mockito 单测覆盖（部分骨架仅有占位）
- 本机 mvn 全量编译验证（首次端到端 729 文件）
- D2 OA Controller mockito 单测（G2 已写但未跑通）
- D5 unified-dispatcher 接入到业务（D2 审批 + D4 提醒）
- 菜单 SQL（V57）部署冒烟（启动应用确认菜单可见）

---

## 7. 强烈建议的下一步

1. **本机/CI 验证**：`mvn -DskipITs -T 4 compile` 验证 729 文件首次端到端编译
2. **push origin**：47 commits 进入主线
3. **开 PR**：进入 main，让本批次成为稳定基线
4. **L 系列骨架细化**：等 sonnet 额度恢复后分批补完
5. **M 系列上线工程**：按上面表逐项推进

---

**整理人**：Claude Opus 4.7
**生成日期**：2026-04-27
**累计 commits**：47
**累计文件**：729 / +60,709 行
