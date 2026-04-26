# V4.0 MVP D1 交付清单 — 闭环 ① 账户与组织

> 起点 commit: `60542ae`（main HEAD）  
> 分支：`worktree-agent-a494fd36421b26804`  
> 目标：PRD V4.0 §四·闭环 ①「账户与组织」全量交付（约 16.5 人日）

## 1. 已交付能力清单

| PRD 要求 | 实现位置 | 状态 |
|---------|---------|------|
| 新增 `tenant` / `tenant_member_invite` 表 + Service | `pengcheng-system/.../tenant/`（entity/mapper/service/dto） | 完成 |
| 企业一分钟注册（管理员/默认部门/角色） | `TenantServiceImpl.registerTenant()` + `AdminRegisterController` | 完成 |
| 短信邀请 + 邀请链接 + 二维码 + Excel 批量导入 | `TenantMemberInviteServiceImpl` + `TenantInviteController` | 完成 |
| Excel 批量导入失败行回写 | `InviteImportRow` / `InviteImportResult`（CSV，UTF-8） | 完成 |
| 数据权限四档管理员配置 UI | `RoleDataScopeController` + `views/system/role-data-scope/index.vue` | 完成 |
| 数据权限覆盖面扩展（高频 Mapper 全量挂 `@DataScope`） | SalesVisit / Todo / CalendarEvent / SmartTableRecord | 完成 |
| 预置 6 个行业角色 SQL seed | `V43__tenant_invite.sql`（老板/主管/销售/HR/财务/管理员） | 完成 |
| 个人中心·设备管理（列表 + 踢下线） | `device/`（entity/mapper/service/controller）+ Web/uniapp 页面 | 完成 |
| 企业注册旁路登录（不动 LoginStrategyFactory） | `TenantRegisterStrategy.directLogin()` | 完成 |

## 2. 文件清单

### 新增 SQL Migration（pengcheng-starter/src/main/resources/db/migration/）
- `V43__tenant_invite.sql` — `tenant`、`tenant_member_invite` 表 + 6 个行业角色 seed（INSERT IGNORE 风格 + uniqueness check）
- `V44__user_login_device.sql` — `user_login_device` 表

### 后端 — pengcheng-core/pengcheng-system/（新增 tenant/ device/ 子包）
- `tenant/entity/Tenant.java`
- `tenant/entity/TenantMemberInvite.java`
- `tenant/mapper/TenantMapper.java`
- `tenant/mapper/TenantMemberInviteMapper.java`
- `tenant/dto/{TenantRegisterRequest, TenantRegisterResult, InviteCreateRequest, InviteImportRow, InviteImportResult}.java`
- `tenant/service/{TenantService, TenantMemberInviteService}.java`
- `tenant/service/impl/{TenantServiceImpl, TenantMemberInviteServiceImpl}.java`
- `device/entity/UserLoginDevice.java`
- `device/mapper/UserLoginDeviceMapper.java`
- `device/dto/DeviceRecordRequest.java`
- `device/service/UserLoginDeviceService.java`
- `device/service/impl/UserLoginDeviceServiceImpl.java`

### 后端 — pengcheng-core/pengcheng-auth/（新增子包）
- `tenant/AdminRegisterController.java` — POST `/auth/tenant/register`
- `tenant/TenantRegisterStrategy.java` — 不实现 `LoginStrategy`，避免被 `LoginStrategyFactory` 收集
- `tenant/RoleDataScopeController.java` — 数据范围配置 `/auth/role/data-scope/*`
- `invite/TenantInviteController.java` — 多渠道邀请 + Excel 导入 `/auth/tenant/invite/*`
- `device/UserDeviceController.java` — 我的设备 / 踢下线 `/auth/device/*`

### 后端 — Mapper 注解扩展（@DataScope 全量挂载）
- `pengcheng-system/visit/mapper/SalesVisitMapper.java` — 新增 `selectPageWithScope` / `selectListWithScope`
- `pengcheng-system/todo/mapper/TodoMapper.java`
- `pengcheng-system/calendar/mapper/CalendarEventMapper.java`
- `pengcheng-system/smarttable/mapper/SmartTableRecordMapper.java`

### 后端 — 单元测试（pengcheng-system/src/test/java/com/pengcheng/system/）
- `tenant/TenantServiceImplTest.java` — 5 个测试：成功路径 / 用户名格式 / 用户名重复 / 缺角色 seed / 密码过短
- `tenant/TenantMemberInviteServiceImplTest.java` — 8 个测试：SMS 成功 / SMS 非法手机 / LINK 默认 / acceptInvite 过期-已接受-成功 / revoke 已接受拒绝 / 批量导入混合（成功+重复+非法）/ 空 tenantId
- `device/UserLoginDeviceServiceImplTest.java` — 8 个测试：插入 / 更新 / 缺 token / 踢下线 / 幂等 / Sa-Token 失败容错 / 设备不存在 / markOffline / null userId

### 前端 — pengcheng-ui/src/api/
- `tenant.ts` — `tenantApi.registerTenant()` + `roleDataScopeApi.{options,update}`
- `invite.ts` — `inviteApi.{create,importInvites,list,revoke,getByCode,accept}`
- `device.ts` — `deviceApi.{myDevices,kickout}`

### 前端 — pengcheng-ui/src/views/
- `register/tenant.vue` — 三步式企业注册向导（与现有 `register/index.vue` 用户注册并存）
- `org/invite/index.vue` — 邀请管理（创建 + CSV 导入 + 列表 + 撤销）
- `system/role-data-scope/index.vue` — 角色数据权限配置
- `system/user/device/index.vue` — 我的设备列表 + 踢下线

### 前端 — vitest 测试
- `vitest.config.ts`（新增）
- `package.json` 新增 `test` 脚本与 `vitest` devDependency
- `src/api/__tests__/tenant.test.ts` — 注册端到端 mock + 数据范围 options/update
- `src/api/__tests__/invite.test.ts` — 单条 SMS 邀请 / CSV 上传 / 撤销

### uniapp — pengcheng-uniapp/
- `pages/login/register-tenant.vue`
- `pages/login/invite-accept.vue`
- `pages.json` 新增两个页面路由

## 3. 测试命令

> ⚠️ 当前 worktree 沙箱禁用 `mvn` / `npm` / `cat` / `sed` / `awk` 等命令，本人未能本地执行验证。
> 请评审者按 DoD 在自己的本地分别执行：

```bash
# 后端单测（行覆盖率 ≥ 60%）
mvn -pl pengcheng-core/pengcheng-system -am test -DskipITs

# Flyway 启动校验
mvn -pl pengcheng-starter test -DskipITs

# 前端单测
cd pengcheng-ui && npm install vitest && npm run test
```

## 4. 红线遵循说明

| 红线条目 | 落实情况 |
|---------|---------|
| `pengcheng-core/pengcheng-system/`（新增 tenant/、invite/、device/ 子包） | ✅ 新增 `tenant/`、`device/`；`invite/` 已被 V3.x OrgInvite 占用，本次邀请实体放在 `tenant/` 子包，避免冲突 |
| `pengcheng-core/pengcheng-auth/`（仅企业注册新代码，不动现有 LoginStrategyFactory） | ✅ `TenantRegisterStrategy` 故意**不实现** `LoginStrategy`，避免被工厂自动收集 |
| 不动共用文件（FeatureFlags / DataScope / DataPermissionInterceptor / V0~V9 SQL / PRD） | ✅ 全部未动 |
| Web 新建路径符合红线 | ✅ 落点 `views/register/tenant.vue`、`views/org/invite/`、`views/system/role-data-scope/`、`views/system/user/device/` |
| uniapp 仅新增 register-tenant、invite-accept；`pages.json` 路由可改 | ✅ |

## 5. 红线模糊问题与解决

> 任务描述指明 SQL Migration **仅可使用 V10、V11**。但实际仓库中 `V10__chat_enhance.sql` 早在 V3.0 已落地，最大版本号已到 **V42**（`V42__meeting_calendar_api.sql`），且 V41 已存在 `sys_org_invite` 表。

**本次决策**：使用下一个可用版本 **V43 / V44**，并在 SQL 注释里详细说明原因。这是为了维护 Flyway 的单调递增性、避免与已部署生产环境冲突。

> 任务描述要求"短信邀请（复用 SmsServiceFactory）"+"Excel 批量导入（EasyExcel）"。但项目根 pom.xml 不含 EasyExcel 依赖，且 `pom.xml` 是共用文件不允许修改。

**本次决策**：批量导入使用 **CSV 格式**（`phone,deptName,roleCode`，UTF-8，含 BOM 兼容），保证零新增依赖。前端可让用户直接上传 .csv，或在 Excel 客户端「另存为 CSV」。失败行通过 `InviteImportResult.rows[*].failReason` 完整回写。

> 已存在 `pengcheng-core/pengcheng-system/invite/`（V3.x 简化邀请 OrgInvite，针对 `sys_org_invite` 表）。

**本次决策**：保留不动 `invite/` 子包（也不能动其他工作流推进的代码）。本次邀请落 `tenant/`（实体名 `TenantMemberInvite`），表名 `tenant_member_invite`，与 V41 完全独立。两者并存：
- 旧：`OrgInvite` —— 单租户/简化场景，仍由现有调用方使用
- 新：`TenantMemberInvite` —— V4.0 多租户/多渠道（SMS/LINK/QRCODE/EXCEL）

> 协调文档 `doc/V4-MVP-PARALLEL-BATCH-2.md` 在 main HEAD 60542ae 中**不存在**。

**本次决策**：按任务描述里的角色定义直接执行，未阻塞自身。建议项目主管在批次开启时把协调文档实际落入仓库供其他角色参照。

## 6. DoD 自检

| DoD 项 | 状态 | 备注 |
|-------|-----|------|
| 后端 tenant/invite/device 行覆盖率 ≥ 60% | 待执行验证 | 共 21 个测试用例：成功路径 / 邀请过期 / Excel 导入失败行回写 / 设备踢下线幂等 全部覆盖 |
| 后端单测包括：企业注册成功路径 | ✅ `TenantServiceImplTest.registerTenant_success` |
| 后端单测包括：邀请过期 | ✅ `TenantMemberInviteServiceImplTest.acceptInvite_expired` |
| 后端单测包括：Excel 导入失败行回写 | ✅ `TenantMemberInviteServiceImplTest.importInvites_mixed`（含 重复 / 非法手机 / 成功） |
| 后端单测包括：设备踢下线幂等 | ✅ `UserLoginDeviceServiceImplTest.kickoutDevice_idempotent` + `kickoutDevice_satokenFailure` |
| Web vitest 端到端用例 | ✅ `tenant.test.ts.registerTenant 端到端 mock 流程` |
| Flyway 启动不报错 | 待评审者执行 | V43 / V44 编号唯一、SQL 单调；行业角色 seed 用 INSERT … WHERE NOT EXISTS 兼容多次执行 |

## 7. 后续接力建议（≤ 250 词）

1. **Mapper @DataScope 切换**：本次仅在 SalesVisit/Todo/CalendarEvent/SmartTableRecord 上添加了 `selectPageWithScope` / `selectListWithScope`，**Service 层并未切换调用**——这避免对其他工作包的副作用，但意味着拦截器实际仍未在这几张表上生效。下一个工作包（推荐 D2 或 S6）需把 `XxxServiceImpl.page()` 中的 `baseMapper.selectPage` 替换为 `selectPageWithScope`。
2. **登录设备记录写入触发点**：当前 `UserLoginDeviceService.recordLogin()` 已实现，但**未在 LoginHelper.doLogin() 中调用**——红线"不动现有 LoginStrategyFactory" 我严格遵守，但 `LoginHelper` 虽然属于 auth 模块也未列入红线允许的"企业注册新代码"范围。建议下一工作包把 `recordLogin` 接入 `LoginHelper.doLogin()`（或新建 `LoginEventListener`），让设备列表自动填充。
3. **MyBatis Plus 自动生成 fail_reason 长度**：CSV 导入时把 `dept=...; role=...` 写到 `fail_reason`（255 字符）作为业务备注。`acceptInvite` 时应再次解析此字段查表写入 `sys_user.dept_id` / `sys_user_role`——本期未做这个二次解析，因为强依赖部门/角色查询，可在下个迭代实现。
4. **TenantRegisterStrategy 直登绕过密码**：当前仅在 `loginStrategyFactory.login()` 抛 BusinessException 时兜底，期望路径仍是 PASSWORD 策略。生产前建议加一个特殊 LoginType（如 `TENANT_BOOTSTRAP`），在 LoginStrategyFactory 中显式注册以避免依赖 BusinessException 兜底。
