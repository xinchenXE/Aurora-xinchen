-- =============================================================
-- 01b · AI 人设设置：后台菜单 + 资源登记（旧版 Aurora 没有）
--
-- 背景：旧版没有「AI人设设置」后台页面与 /admin/ai/persona 接口。
--       t_ai_persona 表由 01 创建；本脚本补齐后台入口与鉴权登记。
-- 幂等：显式主键 + INSERT IGNORE，可重复执行。
-- 生效条件：后端重启（菜单与资源在启动/登录时读取）。
-- =============================================================

-- 选择数据库（库名不同请改这一处）
USE `aurora`;

-- 1. 后台菜单：系统管理 → AI人设设置（order_num=7，与新版一致；菜单由后端按表生成前端路由）
INSERT IGNORE INTO `t_menu`
  (`id`, `name`, `path`, `component`, `icon`, `create_time`, `update_time`, `order_num`, `parent_id`, `is_hidden`)
VALUES (226, 'AI人设设置', '/ai-persona', '/aiPersona/AiPersona.vue', 'el-icon-myguanyuwo', NOW(), NULL, 7, NULL, 0);

-- 2. 菜单授权：admin(1)
INSERT IGNORE INTO `t_role_menu` (`id`, `role_id`, `menu_id`)
VALUES (2886, 1, 226);

-- 3. 资源：模块父项 + 查看/修改两个受保护接口
INSERT IGNORE INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
VALUES (1189, 'AI人设设置', NULL, NULL, NULL, 0, NOW(), NULL),
       (1190, '查看AI人设', '/admin/ai/persona', 'GET', 1189, 0, NOW(), NULL),
       (1191, '修改AI人设', '/admin/ai/persona', 'PUT', 1189, 0, NOW(), NULL);

-- 4. 资源授权：仅 admin(1)
INSERT IGNORE INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
VALUES (5547, 1, 1190),
       (5548, 1, 1191);

-- 核对（依次应返回 1 / 1 / 3 / 2）
-- SELECT COUNT(*) FROM t_menu          WHERE id=226;
-- SELECT COUNT(*) FROM t_role_menu     WHERE id=2886;
-- SELECT COUNT(*) FROM t_resource      WHERE id IN (1189,1190,1191);
-- SELECT COUNT(*) FROM t_role_resource WHERE id IN (5547,5548);
