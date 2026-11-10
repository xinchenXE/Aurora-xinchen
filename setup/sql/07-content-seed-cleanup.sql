-- =============================================================
-- 本地内容种子数据清理（2026-10-03，队列项 020 · 方向 B）
--
-- 对应创建脚本：aurora-xinchen/sql/content_seed_local.sql
-- 仅删除 id >= 90001 的种子行，**不碰任何既有数据**（既有数据 id 均为小值）。
-- 幂等，可重复执行。
--
-- 注意删除顺序：先删关联表，再删主表。
-- =============================================================

SET NAMES utf8mb4;

DELETE FROM `t_article_tag` WHERE `id` >= 90001 OR `article_id` >= 90001 OR `tag_id` >= 90001;
DELETE FROM `t_article`     WHERE `id` >= 90001;
DELETE FROM `t_tag`         WHERE `id` >= 90001;
DELETE FROM `t_category`    WHERE `id` >= 90001;
DELETE FROM `t_talk`        WHERE `id` >= 90001;
DELETE FROM `t_photo`       WHERE `id` >= 90001;
DELETE FROM `t_photo_album` WHERE `id` >= 90001;

-- 校验：应全部为 0
SELECT 't_article' AS tbl, COUNT(*) AS remaining FROM t_article WHERE id >= 90001
UNION ALL SELECT 't_category', COUNT(*) FROM t_category WHERE id >= 90001
UNION ALL SELECT 't_tag', COUNT(*) FROM t_tag WHERE id >= 90001
UNION ALL SELECT 't_article_tag', COUNT(*) FROM t_article_tag WHERE id >= 90001
UNION ALL SELECT 't_talk', COUNT(*) FROM t_talk WHERE id >= 90001
UNION ALL SELECT 't_photo_album', COUNT(*) FROM t_photo_album WHERE id >= 90001
UNION ALL SELECT 't_photo', COUNT(*) FROM t_photo WHERE id >= 90001;
