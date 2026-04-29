-- ----------------------------
-- V14: Sprint B 收尾 — 菜单 + 权限码注册
-- 关联：销售漏斗 / 报表中心 / 佣金审批多级流 / 工单
-- 父菜单 279=房产业务  1=系统管理
-- ID 段约定：320-339 给本次新菜单（避开现有最大 312）
-- ----------------------------

-- ========== 房产业务 → 销售漏斗 ==========
INSERT INTO `sys_menu` (id, parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
VALUES
  (320, 279, '销售漏斗', 2, '/realty/pipeline', 'realty/pipeline/PipelineKanban', 'realty:pipeline:list', 'TrendingUpOutline', 6, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE name=VALUES(name), permission=VALUES(permission);

INSERT INTO `sys_menu` (id, parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted) VALUES
  (321, 320, '商机创建', 3, NULL, NULL, 'realty:pipeline:create', NULL, 1, 1, 1, 0, 0),
  (322, 320, '阶段流转', 3, NULL, NULL, 'realty:pipeline:move',   NULL, 2, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE permission=VALUES(permission);

-- ========== 房产业务 → 报表中心 ==========
INSERT INTO `sys_menu` (id, parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
VALUES
  (323, 279, '报表中心', 2, '/realty/report-file', 'realty/report-file/ReportFileCenter', 'realty:report:download', 'DocumentOutline', 7, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE name=VALUES(name), permission=VALUES(permission);

-- ========== 房产业务 → 佣金审批多级（按钮权限码补齐） ==========
-- 复用现有 284 成交佣金菜单
INSERT INTO `sys_menu` (id, parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted) VALUES
  (324, 284, '提交审批', 3, NULL, NULL, 'realty:commission:submit',           NULL, 1, 1, 1, 0, 0),
  (325, 284, '主管审批', 3, NULL, NULL, 'realty:commission:approve:manager',  NULL, 2, 1, 1, 0, 0),
  (326, 284, '财务审批', 3, NULL, NULL, 'realty:commission:approve:finance',  NULL, 3, 1, 1, 0, 0),
  (327, 284, '放款',     3, NULL, NULL, 'realty:commission:approve:payment',  NULL, 4, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE permission=VALUES(permission);

-- ========== 系统管理 → 工单 ==========
INSERT INTO `sys_menu` (id, parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
VALUES
  (330, 1, '工单管理', 2, '/system/ticket', 'system/ticket/TicketList', 'sys:ticket:list', 'TicketOutline', 20, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE name=VALUES(name), permission=VALUES(permission);

INSERT INTO `sys_menu` (id, parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted) VALUES
  (331, 330, '提单',   3, NULL, NULL, 'sys:ticket:create',  NULL, 1, 1, 1, 0, 0),
  (332, 330, '分配',   3, NULL, NULL, 'sys:ticket:assign',  NULL, 2, 1, 1, 0, 0),
  (333, 330, '处理',   3, NULL, NULL, 'sys:ticket:handle',  NULL, 3, 1, 1, 0, 0),
  (334, 330, '关闭',   3, NULL, NULL, 'sys:ticket:close',   NULL, 4, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE permission=VALUES(permission);

-- ========== 给超管角色（id=1）授权 — 复用现有 sys_role_menu ==========
INSERT INTO `sys_role_menu` (role_id, menu_id) VALUES
  (1, 320), (1, 321), (1, 322),
  (1, 323),
  (1, 324), (1, 325), (1, 326), (1, 327),
  (1, 330), (1, 331), (1, 332), (1, 333), (1, 334)
ON DUPLICATE KEY UPDATE role_id=role_id;
