# 旧版 → 本分支 增量升级包

用于**已经用旧版 aurora 初始化脚本建过库**的环境（24 张表，无 `t_ai_persona`）。

> 全新环境不要用本目录，直接导入 `../aurora-init.sql`。

## 执行顺序

| 顺序 | 文件 | 作用 | 依赖 |
| --- | --- | --- | --- |
| 1 | `01-add-ai-persona-table.sql` | 新建 `t_ai_persona` 表并写默认行 | — |
| 2 | `01b-add-ai-persona-menu.sql` | 补后台菜单 `AI人设设置`（`t_menu` 226）+ 资源/授权（1189~1191、5547/5548） | 1 |
| 3 | `02-ai-persona-extend.sql` | 为旧表补 `role_name` / `greeting` 列 | 1 |
| 4 | `03-ai-chat-resource-auth.sql` | 登记 `/ai/chat`、`/ai/chat/stream` 为需登录资源 | 1 |
| 5 | `04-photo-upload-resource-auth.sql` | 补登记照片/相册封面上传接口（堵匿名上传） | — |
| 6 | `05-log-retention-cleanup.sql` | 登记 90 天日志清理任务（已修正 `job_group='默认'`） | 后端含 `clearExpiredLogs()` |
| 7 | `06-content-seed-local.sql`（可选） | 写入**虚构**演示内容，ID 从 90001 起 | **旧库有真实内容时不要执行** |
| 8 | `07-content-seed-cleanup.sql`（可选） | 删除第 7 步的演示内容 | 7 |

`00-upgrade-all.sql` 是 1~6 的**内联合并版**（自带 `USE aurora`，不依赖 `SOURCE` 与工作目录）。

## 执行方式

一次执行（**推荐**）：

```bash
docker exec -i aurora-mysql mysql -uroot -p'<口令>' < setup/sql/upgrade-from-old/00-upgrade-all.sql
```

用 GUI 工具（Navicat / DBeaver / phpMyAdmin）时：**先选中 `aurora` 库**，再把 `00-upgrade-all.sql` 整体运行。
若库名不是 `aurora`，改文件里的 `USE` 行与 `02` 中的 `TABLE_SCHEMA='aurora'`。

逐个执行（排错时用）：

```bash
for f in setup/sql/upgrade-from-old/0[1-5]*.sql; do
  echo ">> $f"
  docker exec -i aurora-mysql mysql -uroot -p'<口令>' < "$f" || break
done
```

## 执行后核对

```sql
SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA='aurora' AND TABLE_NAME='t_ai_persona';       -- 1
SHOW COLUMNS FROM t_ai_persona LIKE 'role_name';                    -- 1 行
SHOW COLUMNS FROM t_ai_persona LIKE 'greeting';                     -- 1 行
SELECT COUNT(*) FROM t_menu     WHERE id = 226;                     -- 1
SELECT COUNT(*) FROM t_resource WHERE id BETWEEN 1189 AND 1196;     -- 8
SELECT COUNT(*) FROM t_job      WHERE invoke_target='auroraQuartz.clearExpiredLogs'; -- 1，组=默认
```

**然后重启后端**（鉴权资源在 `@PostConstruct` 加载，Quartz 任务启动时读取）。

## 升级包覆盖范围（已对旧基线逐行核对）

| 项 | 旧基线 → 新版 | 由哪个脚本补 |
| --- | --- | --- |
| 表 | 24 → 25（新增 `t_ai_persona`） | 01 |
| 列 | `t_ai_persona` 的 `role_name` / `greeting` | 01 + 02 |
| 菜单 | `t_menu` 226 + `t_role_menu` 2886 | 01b |
| 资源 | 1189~1196 + 授权 5547~5556 | 01b、03、04 |
| 定时任务 | 新增 90 天日志清理任务 | 05 |
| 列差异 | `t_article.article_abstract`（旧有新无） | **保留不删**，代码零引用 |

> 新版初始化脚本里多出的 `t_job_log` / `t_operation_log` / `t_exception_log` 行是**开发机日志**，不是结构差异，升级时无需导入。

## 与旧版的兼容说明

| 项 | 说明 |
| --- | --- |
| `t_article.article_abstract` | 旧库有、新代码不再读写；**保留不删**，删列会丢历史摘要。 |
| 库名 | `02-ai-persona-extend.sql` 内 `TABLE_SCHEMA='aurora'` 写死；库名不同时先改这一处。 |
| 配置 | 旧 Nacos 配置缺新键（`spring.data.redis.*` 新前缀、`aurora.ai.chat.*`、`qq.app-id` 等），按 `../nacos-template.yml` 补齐。 |
| 前端 | 新版前台需要新的接口与字段；前后端应同批升级，避免旧前端调用新接口。 |
| 回滚 | 本包只新增表/行与列，回滚 = 删除新增资源行与任务行（谨慎）或恢复备份；导入前建议先备份。 |
