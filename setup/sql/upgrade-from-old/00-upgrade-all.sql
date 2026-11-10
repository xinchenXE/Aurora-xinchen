-- =============================================================
-- 旧版 Aurora 博客 → 本分支（Java 25 / Spring Boot 3）增量升级包（自包含版）
--
-- 本文件 = 01 + 02 + 03 + 04 + 05 的合并版，不依赖 SOURCE 与工作目录：
--   docker exec -i aurora-mysql mysql -uroot -p'<口令>' < setup/sql/upgrade-from-old/00-upgrade-all.sql
-- 也可在 Navicat / DBeaver / phpMyAdmin 中先选中 `aurora` 库再整体运行。
--
-- 全部语句幂等，可重复执行。
-- 执行后**必须重启后端**（资源鉴权 @PostConstruct 加载；Quartz 任务启动时读取）。
-- 本地开发可选：06/07（虚构演示内容），需单独执行，旧库有真实内容时不要执行。
-- =============================================================

-- 选择数据库（库名不同请改这一处）
USE `aurora`;


-- ==================== 以下来自 01-add-ai-persona-table.sql ====================


-- =============================================================
-- 01 · 新增 t_ai_persona 表（旧版 Aurora 无此表）
--
-- 用途：后台「AI 人设设置」页面的数据表，承载系统提示词、角色名与开场白。
-- 兼容：02-ai-persona-extend.sql 会为「已存在但缺列」的旧表补 role_name / greeting；
--       本脚本对所有情况幂等。
-- =============================================================

CREATE TABLE IF NOT EXISTS `t_ai_persona` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_name` varchar(100) DEFAULT NULL COMMENT '角色名称，空则用 Nacos 默认',
  `greeting` varchar(255) DEFAULT NULL COMMENT '开场白，空则用 Nacos 默认',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '人设提示词',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- 空表时写入默认人设（提示词留空 = 回退 Nacos 的 aurora.ai.chat.system-prompt）
INSERT INTO `t_ai_persona` (`id`, `role_name`, `greeting`, `system_prompt`, `create_time`, `update_time`)
SELECT 1, '', '', '', NOW(), NULL
WHERE NOT EXISTS (SELECT 1 FROM `t_ai_persona` WHERE `id` = 1);

-- 核对（应返回 1）
-- SELECT COUNT(*) FROM `t_ai_persona`;


-- ==================== 以下来自 01b-add-ai-persona-menu.sql ====================

-- =============================================================
-- 01b · AI 人设设置：后台菜单 + 资源登记（旧版 Aurora 没有）
--
-- 背景：旧版没有「AI人设设置」后台页面与 /admin/ai/persona 接口。
--       t_ai_persona 表由 01 创建；本脚本补齐后台入口与鉴权登记。
-- 幂等：显式主键 + INSERT IGNORE，可重复执行。
-- 生效条件：后端重启（菜单与资源在启动/登录时读取）。
-- =============================================================


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


-- ==================== 以下来自 02-ai-persona-extend.sql ====================


-- =============================================================
-- AI 人设设置扩展：角色名称 + 开场白（2026-08-15）
-- 两字段均为空则回退 Nacos 默认值（aurora.ai.chat.role-name / greeting）
-- 脚本幂等，可重复执行。
-- =============================================================

-- 1. 角色名称列
SELECT COUNT(*) INTO @has_role_name
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'aurora' AND TABLE_NAME = 't_ai_persona' AND COLUMN_NAME = 'role_name';
SET @ddl_role_name = IF(@has_role_name = 0,
    'ALTER TABLE `t_ai_persona` ADD COLUMN `role_name` varchar(100) DEFAULT NULL COMMENT ''角色名称，空则用 Nacos 默认'' AFTER `id`',
    'SELECT ''role_name already exists'' AS msg');
PREPARE stmt FROM @ddl_role_name;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 开场白列
SELECT COUNT(*) INTO @has_greeting
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'aurora' AND TABLE_NAME = 't_ai_persona' AND COLUMN_NAME = 'greeting';
SET @ddl_greeting = IF(@has_greeting = 0,
    'ALTER TABLE `t_ai_persona` ADD COLUMN `greeting` varchar(255) DEFAULT NULL COMMENT ''开场白，空则用 Nacos 默认'' AFTER `role_name`',
    'SELECT ''greeting already exists'' AS msg');
PREPARE stmt FROM @ddl_greeting;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ==================== 以下来自 03-ai-chat-resource-auth.sql ====================


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


-- ==================== 以下来自 04-photo-upload-resource-auth.sql ====================


-- =============================================================
-- 补登记两个 admin 文件上传接口（2026-09-30，P0 安全修复 · ISS-020）
--
-- 背景：差集实测（126 路由 vs 124 登记）发现这两个接口从未登记进 t_resource，
--       按 ResourceRoleAuthorizationManager「未匹配到受管资源即放行」的语义，
--       匿名请求可**完全绕过鉴权**直接进到上传逻辑。
--       运行时实证（后端 8080，无 Authorization 头）：
--         对照组 GET /admin/photos   → {"code":40001,"message":"用户未登录"}  ✅ 被拒
--         本脚本两条 POST .../upload → {"code":51000,"message":"文件上传失败"} ❌ 已越过鉴权
--       （失败只因未附文件；带真实 MultipartFile 即写入 MinIO）
--
-- 授权口径：对齐同模块的**写操作** —— 照片模块的写接口（1111 保存照片、
--           1117 上传相册封面 等）均只授权 role 1(admin)；读接口才有 1,14。
--           故本脚本两条同样只授 admin，不给 user/test。
--
-- 幂等（INSERT IGNORE + 显式主键，照 ai_chat_resource_auth.sql 模式），可重复执行。
-- 生效条件：**后端重启**（ResourceRoleAuthorizationManager @PostConstruct 只在启动时加载）。
-- 同步要求：模板库 `aurora-xinchen.sql` 需一并同步（见同目录 README 或 AGENTS.md 四节 1）。
-- =============================================================

-- 1. 受保护资源（is_anonymous=0 → 进入资源-角色鉴权链）
--    parent_id 沿用同模块父项：1059=照片，1062=相册
INSERT IGNORE INTO `t_resource` (`id`, `resource_name`, `url`, `request_method`, `parent_id`, `is_anonymous`, `create_time`, `update_time`)
VALUES (1195, '上传照片', '/admin/photos/upload', 'POST', 1059, 0, NOW(), NULL),
       (1196, '上传相册封面', '/admin/photos/albums/upload', 'POST', 1062, 0, NOW(), NULL);

-- 2. 角色授权：仅 admin(1) —— 与同模块写操作口径一致
INSERT IGNORE INTO `t_role_resource` (`id`, `role_id`, `resource_id`)
VALUES (5555, 1, 1195),
       (5556, 1, 1196);

-- 3. 核对（执行后应返回 2 行，且角色为 1）
-- SELECT r.id, r.url, r.request_method, r.is_anonymous, GROUP_CONCAT(rr.role_id) roles
--   FROM t_resource r LEFT JOIN t_role_resource rr ON r.id = rr.resource_id
--  WHERE r.id IN (1195, 1196) GROUP BY r.id, r.url, r.request_method, r.is_anonymous;


-- ==================== 以下来自 05-log-retention-cleanup.sql ====================


-- ============================================================================
-- 队列项 006：操作日志/异常日志保留 90 天清理任务登记
-- 日期：2026-09-30
-- 拍板口径：t_operation_log 与 t_exception_log 统一保留 90 天
--           （CommonConstant.LOG_RETENTION_DAYS = 90）
-- 幂等：重复执行不会产生重复行（按 job_name 判存）
-- 依赖：AuroraQuartz.clearExpiredLogs()（本队列项新增）
-- ============================================================================

-- 登记清理任务：每日 03:00 执行（避开既有任务：00:00 统计访问量 + 清 job 日志、01:00 清 redis 访客）
-- job_group 必须与既有任务一致（「默认」）；漏写会落 DEFAULT 组，管理端操作必失败（ISS-026）。
INSERT INTO `t_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_time`, `update_time`)
SELECT '清理过期操作/异常日志', '默认', 'auroraQuartz.clearExpiredLogs', '0 0 3 * * ?', 3, 1, 1, NOW(), NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `t_job` WHERE `invoke_target` = 'auroraQuartz.clearExpiredLogs'
);

-- 核对（应返回 1 行，job_group 为「默认」）
-- SELECT id, job_name, job_group, invoke_target, cron_expression, status FROM `t_job`
--  WHERE invoke_target = 'auroraQuartz.clearExpiredLogs';


-- ==================== 执行后核对（逐条粘贴执行）====================
-- SELECT COUNT(*) AS persona_table FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA='aurora' AND TABLE_NAME='t_ai_persona';               -- 期望 1
-- SHOW COLUMNS FROM `t_ai_persona` LIKE 'role_name';                          -- 期望 1 行
-- SHOW COLUMNS FROM `t_ai_persona` LIKE 'greeting';                           -- 期望 1 行
-- SELECT COUNT(*) AS ai_resources FROM t_resource WHERE id IN (1193,1194);     -- 期望 2
-- SELECT COUNT(*) AS photo_resources FROM t_resource WHERE id IN (1195,1196);  -- 期望 2
-- SELECT id, job_group FROM t_job WHERE invoke_target='auroraQuartz.clearExpiredLogs'; -- 期望 1 行，组=默认
