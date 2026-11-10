
-- 选择数据库（库名不同请改这一处）
USE `aurora`;
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
