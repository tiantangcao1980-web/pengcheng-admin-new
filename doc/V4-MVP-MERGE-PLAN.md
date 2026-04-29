# V4.0 MVP 第二批五路合并预案（D1～D5）

> 版本：rev.1（2026-04-26）
> 维护人：Claude（worktree `claude/recursing-spence-e31bd4`）
> 上游基线：HEAD `60542ae`（origin/main）
> 配套：[`PRD-V4.0-新产品需求文档.md`](./PRD-V4.0-新产品需求文档.md)、[`V4-MVP-PARALLEL-BATCH-2.md`](./V4-MVP-PARALLEL-BATCH-2.md)

---

## 1. 五路分支与交付概览

| Owner | 分支 | HEAD（amend 后） | 文件 | 新增行 | Flyway V 编号（已重排） |
|-------|------|------------------|------|-------|------------------------|
| D1 闭环① 账户与组织 | `worktree-agent-a494fd36421b26804` | `e765cf8` | 47 | +3608 | V43__tenant_invite, V44__user_login_device ✅ |
| D2 闭环② OA | `worktree-agent-ad4f9c0d45c23af0a` | `dd10c4d` | 60 | +5369 | V45__attendance_shift, V46__attendance_correction, V47__approval_flow ✅ |
| D3 闭环③ CRM | `worktree-agent-a4013e90b5395e7cf` | `dc4c415` | 74 | +4169 | V48__crm_lead, V49__crm_custom_field, V50__customer_visit_media, V51__customer_tag, V52__customer_realty_ext ✅ |
| D4 闭环④ AI Copilot | `worktree-agent-a7f9e021a9eef0b64` | `31fe85b` | 55 | +3747 | V53__ai_reminder_rule, V54__ai_copilot_action_log ✅ |
| D5 闭环⑤ 移动办公 | `worktree-agent-a2ca3dc40b69b7da0` | `5ebc9bb` | 43 | +4088 | V55__push_channel_log, V56__subscribe_msg_template ✅（已迁至 db/migration/） |
| **合计** | — | — | **279** | **+20,981** | V43-V56 连续无冲突 |

> rev.2 状态：§2.1 V 编号重排 + §2.2 D5 路径迁移**已执行完毕**（amend commits）。后续接力关注 §2.3（D1 invite 融合）+ §2.4（pom 合并）+ §2.5（共享集成点）。

---

## 2. 合并阶段必修问题

### 2.1 Flyway V 编号大撞车（5 路全部从 V43 起步）

仓库真实 Flyway 目录最高 V42。五路 worktree 各自不可见对方工作，全部从 V43 开始数。**必须在合并前对每路重命名 V 文件**，不可向 origin/main 推送原始 V 编号。

**建议重排（按完成顺序优先级）**：

| Owner | 原 V | 新 V | 说明 |
|-------|------|------|------|
| **D1** | V43, V44 | **V43, V44** | 先合，保留原编号 |
| **D2** | V43, V44, V45 | **V45, V46, V47** | +2 |
| **D3** | V43~V47 | **V48~V52** | +5 |
| **D4** | V43, V44 | **V53, V54** | +10 |
| **D5** | sql/V22, sql/V23 | **V55, V56**（同时迁路径，见 §2.2） | +33 + 路径修正 |

操作脚本（每路在自己 worktree 里执行 + amend commit）：
```bash
# D2 worktree
cd .claude/worktrees/agent-ad4f9c0d45c23af0a
git mv pengcheng-starter/src/main/resources/db/migration/V43__attendance_shift.sql      pengcheng-starter/src/main/resources/db/migration/V45__attendance_shift.sql
git mv pengcheng-starter/src/main/resources/db/migration/V44__attendance_correction.sql pengcheng-starter/src/main/resources/db/migration/V46__attendance_correction.sql
git mv pengcheng-starter/src/main/resources/db/migration/V45__approval_flow.sql         pengcheng-starter/src/main/resources/db/migration/V47__approval_flow.sql
git commit --amend --no-edit
```
（D3/D4/D5 同理）

### 2.2 D5 SQL 路径错位

D5 把两个 migration 写到了 `sql/V22__*.sql` 和 `sql/V23__*.sql`（即旧的初始化目录），不是 Flyway 实际目录。合并前必须：
```bash
cd .claude/worktrees/agent-a2ca3dc40b69b7da0
mkdir -p pengcheng-starter/src/main/resources/db/migration
git mv sql/V22__push_channel_log.sql      pengcheng-starter/src/main/resources/db/migration/V55__push_channel_log.sql
git mv sql/V23__subscribe_msg_template.sql pengcheng-starter/src/main/resources/db/migration/V56__subscribe_msg_template.sql
git commit --amend --no-edit
```

### 2.3 D1 invite 与已有 OrgInvite 融合

`pengcheng-system/invite/`（OrgInviteService、OrgInvite 实体、`sys_org_invite` 表 V41）已由 codex p1-dev 落地。D1 在 `pengcheng-auth/invite/` 里新增的"四渠道扩展"（短信/链接/二维码/Excel）应作为 OrgInvite 的**渠道适配层**，而不是平行实现。

合并审查清单：
- [ ] 对比 D1 `pengcheng-auth/invite/` 与 main `pengcheng-system/invite/OrgInviteService` 的字段命名
- [ ] 把 D1 的"短信邀请、邀请链接、二维码生成、Excel 批量导入"4 个 channel sender 改造成 OrgInvite 的**多 sender 策略**（依赖注入 OrgInviteService）
- [ ] D1 V43__tenant_invite.sql 的 `tenant_member_invite` 表如和 `sys_org_invite` 字段重合，建议合并为单一表 `sys_org_invite` 并添加 `channel`、`tenant_id`、`expires_at` 字段
- [ ] 保留 D1 的 `tenant`（企业/租户）+ `user_login_device` 两张表（无现存重复）

### 2.4 父 pom.xml 子模块声明合并

D2 / D3 都在 `pengcheng-core/pom.xml` 加了 `<module>` 行：
- D2: `<module>pengcheng-oa</module>`
- D3: `<module>pengcheng-crm</module>` + 顶层 `pom.xml` dependencyManagement 增加 pengcheng-crm

合并时两个声明都保留即可（互不冲突）。

### 2.5 共享集成点（合并完后立即处理）

- **审批回调统一**：D5 的 `ChannelPushService` 应被 D2 的 `AppApprovalController` 调用（取代旧 `NotificationServiceImpl.pushSafe`）
- **AI 提醒推送 Adapter**：D4 的 `ChannelPushPortAdapter` 已写为接口，合并后绑定到 D5 的 `UnifiedPushDispatcher`
- **菜单/路由表**：D1/D2/D3/D4 各自的 Web 视图 + uniapp 页面尚未挂入 `sys_menu` / `pages.json`。建议合并后单独开一个 `chore(menu): V4 MVP 五闭环菜单与路由挂入` PR 统一处理

---

## 3. 推荐合并顺序

```
main (60542ae)
  ↓ 合并 1：D1（基础租户/邀请/设备 — 其他闭环都依赖租户）
main + D1
  ↓ 合并 2：D2（OA 流程引擎 — D5 推送依赖）
main + D1 + D2
  ↓ 合并 3：D3（CRM — 独立闭环）
main + D1 + D2 + D3
  ↓ 合并 4：D5（统一推送通道 — D4 提醒依赖）
main + D1 + D2 + D3 + D5
  ↓ 合并 5：D4（AI Copilot — 依赖推送 + 业务动作目标）
main + D1 + D2 + D3 + D5 + D4
  ↓ 合并 6：chore(menu) 菜单与路由挂入
main（V4.0 MVP 五闭环 done）
```

每个合并步骤的检查项：
1. `mvn -pl <相应模块> -am test -DskipITs` 通过
2. Flyway 本地启动 `pengcheng-starter` 不报错
3. `pengcheng-ui` `pnpm vue-tsc --noEmit` 不引入新错误
4. uniapp `pages.json` 路由完整（D5 的 manifest.json 修改与 D4 的 pages.json 修改不冲突）

---

## 4. 后续接力清单

| # | 事项 | Owner 建议 |
|---|------|-----------|
| 1 | **执行本预案 §2 V 编号重排 + D5 路径迁移**（5 个 amend commit） | 协调人 |
| 2 | **D1 invite 与 OrgInvite 融合 PR**（融合方案见 §2.3） | 后端 Lead |
| 3 | **菜单与路由挂入 PR**（5 闭环 + uniapp pages.json） | 前端 Lead |
| 4 | **starter 模块装配 PR**：`UnifiedPushDispatcher` + `ChannelPushService` + `ReminderScheduler` 注入 Spring 上下文 | 后端 Lead |
| 5 | **小程序订阅消息 + WebInbox sender 实现**（D5 留 SPI 接口） | 后端 #2 |
| 6 | **realty facade 双写 ext**：让 `CustomerServiceImpl.create/update` 写完主表后调用 `CustomerRealtyExtService.upsert`（D3 留扩展点） | 后端 #3 |
| 7 | **EAV 热字段冗余**：客户列表高频筛选项冗余到主表（PRD §8.2 已警示） | 后端 #3 |
| 8 | **mvn 全量验证**：每路单测在主机环境跑 `-am test -DskipITs` | DevOps |
| 9 | **D2 Controller 补齐**：4 个 OA REST Controller（D2 子 agent 没写避免侵入他人红线） | 后端 #2 |
| 10 | **JPush 真接入 + iOS 证书 + 百度 OCR Provider**：填入 manifest.json 与 OcrProvider 实现 | 移动 |

---

## 5. 风险与备注

- **测试未跑**：5 路子 agent 均因沙箱限制未运行 mvn / vitest，**所有测试代码静态合规但未编译验证**。合并任意一路前必须本机/CI 跑通 `mvn -pl <module> -am test -DskipITs`。
- **D1/D4 子 agent 触达 API 额度**：报告未输出但代码完整 commit。后续如发现遗漏，可在合并 PR 描述里补充。
- **菜单 + 设备会话**：D1 数据权限扩展只挂了 4 个 Mapper（CalendarEvent/SmartTableRecord/Todo/SalesVisit），其他高频 Mapper 待后续 PR 完成全量覆盖。
