-- =============================================================
-- AI 对话模块资源登记与角色授权（2026-08-15，P0 安全修复）
-- 背景：/ai/chat 与 /ai/chat/stream 未登记进 t_resource，
--       按 ResourceRoleAuthorizationManager「未匹配即放行」语义被匿名调用，
--       且请求体可携带 model 参数指定模型，直接消耗 Nacos 配置的 API Key。
-- 本脚本将两个接口登记为受保护资源（is_anonymous=0）并授权给
-- admin(1) / user(2) / test(14) 三个角色。
-- /ai/assistant 保持匿名（仅返回展示文案，设计如此，不登记）。
-- 脚本幂等（INSERT IGNORE + 显式主键），可重复执行。
-- 生效条件：后端重启（ResourceRoleAuthorizationManager @PostConstruct 加载）。
-- =============================================================

-- 1. 模块父项（对齐「AI人设设置」1189 的登记方式）
INSERT IGNORE INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
VALUES (1192, 'AI对话模块', NULL, NULL, NULL, 0, NOW(), NULL);

-- 2. 受保护资源（is_anonymous=0 → 进入资源-角色鉴权链）
INSERT IGNORE INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
VALUES (1193, 'AI对话', '/ai/chat', 'POST', 1192, 0, NOW(), NULL),
       (1194, 'AI流式对话', '/ai/chat/stream', 'GET', 1192, 0, NOW(), NULL);

-- 3. 角色授权：admin(1) / user(2) / test(14) × 两资源
INSERT IGNORE INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
VALUES (5549, 1, 1193), (5550, 1, 1194),
       (5551, 2, 1193), (5552, 2, 1194),
       (5553, 14, 1193), (5554, 14, 1194);
