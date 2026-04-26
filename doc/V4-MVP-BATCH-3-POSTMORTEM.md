# V4.0 MVP 第三批 5 路接力 — 复盘 + 抢救报告

> 版本：rev.1（2026-04-26）
> 维护人：Claude（worktree `claude/recursing-spence-e31bd4`）
> 协调人 HEAD：`f51999a`（基于 `60542ae`，累计 318 文件 / +23,245 行）

---

## 1. 事故核心

`Agent` 工具的 `isolation: "worktree"` 模式，**默认从仓库 default branch（origin/main）派生新 worktree**，而非主 agent 当前分支 HEAD。

第三批 5 个 E agent（E1-E5）全部从 `60542ae` 派生，**完全看不到协调人分支上 D1-D5 的合并基线**。结果：
- E1 写了 31 个 stub 视图占据 D1-D5 真实视图路径
- E2 写了 19 个 SPI stub 类，路径与 D4/D5 真实实现不同（造成包级别二义性）
- E3 在 `pengcheng-hr/oa/` 重新实现了 D2 已有的 `pengcheng-oa/` OA 模块（39 文件冗余）
- E4 V43 重建了 D1 V43 已有的 tenant + tenant_member_invite 表
- E5 在 `pengcheng-infra/pengcheng-push/unified/` 重写了 D5 已有的 SPI 接口

## 2. 抢救方案：差量 cherry-pick

对 5 个 E worktree 用 `git diff --name-status 60542ae HEAD` 列出全部 staged 文件，逐文件 cross-check 协调人分支，分类：
- ✅ **NEW**：协调人分支不存在 → 安全采纳
- ⚠️ **COVERS**：协调人分支已有 → 需逐文件 diff 决策

### 2.1 已抢救（commit `f51999a`，41 文件）

| Owner | 采纳的高价值文件 | 类型 |
|-------|-----------------|------|
| **E1** | `V57__v4_mvp_menus.sql`、4 个 `pages/approval/cards/{leave,overtime,expense,outing}.vue` | 5 NEW |
| **E4** | `V44__org_invite_channel_merge.sql`、`OrgInviteChannelController`、`TenantContextHolder`、`TenantInterceptor`、`InviteChannelSender` SPI、`InviteChannel` 常量、4 个 channel Sender（auth 模块）、`OrgInvite.java` +4 字段、`OrgInviteService` +4 方法、`OrgInviteServiceImpl` channel 路由、`OrgInviteServiceImplTest` 调整、`application.yml` +`pengcheng.feature.tenant=false` | 11 NEW + 5 累加式覆盖 |
| **E5** | `jpush/` 6 文件 + `JpushUnifiedSenderTest`、`pengcheng-wechat/subscribe/` 7 文件 + `WechatMpSubscribeSenderTest`、`pengcheng-message/inbox/` 3 文件 + `WebInboxSenderTest`、`UnifiedPushAutoConfiguration` | 20 NEW |

合计 **41 文件**，全部 commit 到协调人分支。

### 2.2 已弃用（不合并）

| Owner | 弃用原因 |
|-------|----------|
| **E1 stub 视图** | 31 个 stub 文本会**覆盖 D1-D5 真实视图** |
| **E1 router/index.ts + pages.json** | 基于 60542ae 修改，没有 D1-D5 路径，需要在合并基线上重做 |
| **E2 全部** | 19 SPI stub 路径错位（pengcheng-system/approval/ vs D2 真实包 pengcheng-oa/flow/service/）；`V4MvpAutoConfiguration` import 错；保留作为参考实现，不进 commit |
| **E3 全部** | `pengcheng-hr/oa/` 39 文件重做 D2 `pengcheng-oa/`；4 个 Controller 注入错误包；保留作为参考实现 |
| **E4 V43__tenant_invite.sql** | D1 V43 已有，重建会覆盖 |
| **E5 unified/ NoOp + ChannelInbox/SubscribeSender 接口** | D5 已有同名接口；E5 NoOp 占位与 D5 重叠 |

E2/E3/E4/E5 的 WIP commit 仍保留在各自 `worktree-agent-*` 分支，可后续 cherry-pick 但建议不再投入。

## 3. 当前协调人分支状态

```
f51999a feat(salvage-e1-e4-e5): cherry-pick V4 MVP 第三批高价值改动
f9a31df Merge D5: V4.0 闭环⑤ 移动办公
abdac3b Merge D4: V4.0 闭环④ AI Copilot（解决 vitest 冲突）
cd7db7f Merge D3: V4.0 闭环③ CRM（解决 pom 冲突）
5fef839 Merge D2: V4.0 闭环② OA
9efb70d Merge D1: V4.0 闭环① 账户与组织
73160f3 docs(coord): 合并预案 rev.2 — V 编号重排已执行
e0c898f docs(coord): 五路合并预案 + 红线 rev.2
2e69b9f docs(coord): 五路并行协调文档
60542ae Merge pull request #2 (origin/main)
```

V43-V57 Flyway 编号连续无冲突。

## 4. 必须接力的 4 项工作（F 系列）

> ⚠️ **必须在协调人分支上直接工作**，**禁用 `isolation: "worktree"`** —— 否则 worktree 又会从 origin/main 派生。
> 改为：直接由主 agent 在 `recursing-spence-e31bd4` 工作树上做，或派非隔离 agent。

| # | 任务 | 文件区域 | 价值 |
|---|------|---------|------|
| **F1** | **路由 + pages 挂入**：在 D1-D5 已存在的 router/index.ts、pages.json 上添加 V4 闭环视图 + 页面记录（不覆盖原文件） | `pengcheng-ui/src/router/`、`pengcheng-uniapp/pages.json` | V57 菜单 SQL 已挂入但前端不可点 → F1 完成后菜单点击可路由到真实视图 |
| **F2** | **OA 4 个 REST Controller**：基于 D2 真实 Service（`pengcheng-oa.flow.service.ApprovalFlowEngine` 等）写 4 个 Controller，URL 与 `pengcheng-ui/src/api/oa*.ts` 对齐 | `pengcheng-api/pengcheng-admin-api/src/main/java/.../controller/oa/` | 前端 `/admin/oa/*` 调用目前 404 → F2 完成后端点可用 |
| **F3** | **starter 装配重做**：基于 D2 `pengcheng-oa/`、D4 `pengcheng-ai/reminder/copilot-action/cost/`、D5 `pengcheng-message/channel/subscribe/` + `pengcheng-push/unified/`、E5 抢救进的 jpush/wechat/inbox sender — 写 V4MvpAutoConfiguration 注入真实 Bean；2 个 Quartz Job（ReminderRule + ApprovalTimeoutSweep）；冒烟测断言所有 Bean 可注入 | `pengcheng-starter/src/main/java/.../config/`、`pengcheng-job/src/main/java/.../task/` | Spring 上下文不能正常装配 → F3 完成后启动器跑通 |
| **F4** | **push_channel_log 表落库**：D5 V55 已建表，`PushChannelLog` 实体 + Mapper 在 E5 抢救代码里。把 sender 失败回写补上 | E5 抢救进来的 sender 类 + V55 表 | 推送审计闭环完整 |

## 5. F 系列工作量估算

| F | 估时 | 复杂度 | 阻塞 |
|---|------|------|------|
| F1 | 1-2 小时 | 低（合并 router 表 + 写 pages.json 条目） | 无 |
| F2 | 4-5 小时 | 中（按 D2 真实接口写 4 Controller + Mockito 测试） | 必须先理解 D2 真实 Service 签名 |
| F3 | 4-5 小时 | 中（装配 + 冒烟测） | F2 落地后更稳 |
| F4 | 1-2 小时 | 低（在 E5 抢救代码上加 mapper.insert 调用） | 无 |

总计 ~12-14 小时连续工作，可以分多次推进。

## 6. 协调人后续行动建议

1. **不要再用 `isolation: "worktree"`** 派 agent 到协调人分支基础上的工作 —— 工具陷阱已确认，会再犯
2. **改为非隔离派 agent**（默认在主 agent 当前 worktree 工作）—— 若必须用 worktree，先确认 worktree 是从协调人分支派生的
3. **优先做 F1**：路由不通菜单点不到，是体验阻塞最大的项
4. **F2 + F3 一起做**：装配引用 Service，先有 Controller 再装配可避免循环修改
5. **`mvn -DskipITs` 编译验证**未跑：协调人分支 318 文件 +23,245 行从未本地编译，**强烈建议在 push 前本机跑** `mvn -pl pengcheng-starter -am compile`

---

> **触底反思**：worktree 隔离对"独立任务并行"是好的，但对"基于主 agent 当前进展接力"是错的。下一批工作应放弃隔离，改在协调人分支直接做。
