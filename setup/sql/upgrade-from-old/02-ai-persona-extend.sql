
-- 选择数据库（库名不同请改这一处）
USE `aurora`;
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
