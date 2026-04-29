-- ============================================================
-- V57: V4 MVP 五闭环菜单 seed
-- 补充路由/菜单挂入：五大闭环（①账户组织 ②OA ③CRM ④AI助手 ⑤推送通道）
-- 参照 V29/V36 风格，NOT EXISTS 防重复，admin 角色自动获取全量菜单，
-- 非 admin 角色继承父目录权限后同步新子菜单。
-- ============================================================

-- ============================================================
-- 闭环① 账户与组织 — 新增子菜单
--   父目录：组织管理 (已有)  + 系统管理 (id=1)
-- ============================================================

-- 1-A 成员邀请（挂在「组织管理」下）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '成员邀请', 2, '/org/invite', 'org/invite/index', 'org:invite:list', 'PersonAddOutline', 3, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '组织管理' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '成员邀请' AND deleted = 0);

-- 1-B 数据权限配置（挂在「系统管理」下，id=1）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 1, '数据权限配置', 2, '/system/role-data-scope', 'system/role-data-scope/index', 'system:role:datascope', 'ShieldCheckmarkOutline', 3, 1, 1, 0, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '数据权限配置' AND deleted = 0);

-- 1-C 设备管理（挂在「系统管理」下，用户中心子入口）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 1, '设备管理', 2, '/system/user/device', 'system/user/device/index', 'system:user:device', 'PhonePortraitOutline', 4, 1, 1, 0, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '设备管理' AND deleted = 0);

-- ============================================================
-- 闭环② 日常办公 OA — 新增「OA 日常办公」一级目录及子菜单
-- ============================================================

-- 新建「OA 日常办公」一级目录（type=1）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 0, 'OA 日常办公', 1, '', NULL, NULL, 'BusinessOutline', 35, 1, 1, 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'OA 日常办公' AND parent_id = 0 AND deleted = 0);

-- 2-A 班次管理
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '班次管理', 2, '/oa/shift', 'oa/shift/index', 'oa:shift:list', 'TimeOutline', 1, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = 'OA 日常办公' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '班次管理' AND deleted = 0);

-- 2-B 补卡申请
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '补卡申请', 2, '/oa/correction', 'oa/correction/index', 'oa:correction:list', 'CreateOutline', 2, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = 'OA 日常办公' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '补卡申请' AND deleted = 0);

-- 2-C 审批模板（管理员配置）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '审批模板', 2, '/oa/approval-template', 'oa/approval-template/index', 'oa:approval-template:list', 'DocumentTextOutline', 3, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = 'OA 日常办公' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '审批模板' AND deleted = 0);

-- 2-D 审批流程（管理员可视化配置）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '审批流程', 2, '/oa/approval-flow', 'oa/approval-flow/index', 'oa:approval-flow:list', 'GitNetworkOutline', 4, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = 'OA 日常办公' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '审批流程' AND deleted = 0);

-- ============================================================
-- 闭环③ 客户管理 CRM — 新增「CRM 客户」一级目录及子菜单
-- ============================================================

-- 新建「CRM 客户」一级目录（type=1）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 0, 'CRM 客户', 1, '', NULL, NULL, 'PeopleOutline', 36, 1, 1, 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'CRM 客户' AND parent_id = 0 AND deleted = 0);

-- 3-A 线索管理
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '线索管理', 2, '/crm/lead', 'crm/lead/index', 'crm:lead:list', 'FunnelOutline', 1, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = 'CRM 客户' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '线索管理' AND deleted = 0);

-- 3-B 自定义字段（管理员配置字段与校验规则）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '自定义字段', 2, '/crm/custom-field', 'crm/custom-field/index', 'crm:custom-field:list', 'OptionsOutline', 2, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = 'CRM 客户' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '自定义字段' AND deleted = 0);

-- 3-C 客户标签
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '客户标签', 2, '/crm/tag', 'crm/tag/index', 'crm:tag:list', 'PricetagOutline', 3, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = 'CRM 客户' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '客户标签' AND deleted = 0);

-- 3-D 导入导出
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, '导入导出', 2, '/crm/import-export', 'crm/import-export/index', 'crm:import-export:list', 'CloudUploadOutline', 4, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = 'CRM 客户' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '导入导出' AND deleted = 0);

-- ============================================================
-- 闭环④ AI 智能助手 — 追加 Copilot 配置（挂在「智能助手」下）
-- ============================================================

-- 4-A Copilot 配置
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT p.id, 'Copilot 配置', 2, '/ai/copilot-config', 'ai/copilot-config/index', 'ai:copilot:config', 'SparklesOutline', 8, 1, 1, 0, 0
FROM sys_menu p WHERE p.name = '智能助手' AND p.parent_id = 0 AND p.deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = 'Copilot 配置' AND deleted = 0);

-- ============================================================
-- 闭环⑤ 移动办公 / 推送通道（挂在「系统管理」下，替代已有渠道推送）
-- 注意：「渠道推送」在 V36 已挂到系统管理，此处新增「推送通道」配置页
-- ============================================================

-- 5-A 推送通道（三供应商工厂配置 UI，sort=10，避开 V36 已占用的 sort=8）
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
SELECT 1, '推送通道', 2, '/system/push-channel', 'system/push-channel/index', 'system:push-channel:list', 'NotificationsOutline', 10, 1, 1, 0, 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE name = '推送通道' AND deleted = 0);

-- ============================================================
-- RBAC：为非 admin 角色同步新菜单
-- admin 角色通过 getUserMenuTree 中的 roleCodes.contains("admin") 获取全量菜单，无需 sys_role_menu 记录。
-- 以下为已拥有父目录权限的非 admin 角色自动继承子菜单。
-- ============================================================

-- 组织管理子菜单 → 成员邀请
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name = '成员邀请' AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '组织管理' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 系统管理子菜单 → 数据权限配置、设备管理、推送通道
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name IN ('数据权限配置', '设备管理', '推送通道') AND m.deleted = 0
  AND rm.menu_id = 1
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- OA 日常办公子菜单 → 所有 OA 子菜单（任何拥有 OA 日常办公父菜单的角色）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.deleted = 0 AND m.type = 2
  AND m.parent_id = (SELECT id FROM sys_menu WHERE name = 'OA 日常办公' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = 'OA 日常办公' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- CRM 客户子菜单 → 所有 CRM 子菜单
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.deleted = 0 AND m.type = 2
  AND m.parent_id = (SELECT id FROM sys_menu WHERE name = 'CRM 客户' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = 'CRM 客户' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);

-- 智能助手子菜单 → Copilot 配置
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.id
FROM sys_role_menu rm
CROSS JOIN sys_menu m
WHERE m.name = 'Copilot 配置' AND m.deleted = 0
  AND rm.menu_id = (SELECT id FROM sys_menu WHERE name = '智能助手' AND parent_id = 0 AND deleted = 0 LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = rm.role_id AND rm2.menu_id = m.id);
