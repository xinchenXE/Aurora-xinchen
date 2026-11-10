
-- 选择数据库（库名不同请改这一处）
USE `aurora`;
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
