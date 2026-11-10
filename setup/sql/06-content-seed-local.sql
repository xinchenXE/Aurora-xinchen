-- =============================================================
-- 本地内容数据种子脚本（2026-10-03，队列项 020 · 方向 B）
--
-- 目的：本地 `aurora` 库内容表全 0（ISS-027），前台首页/归档/标签整页无内容。
--       本脚本写入**虚构**的种子内容，恢复首屏与各列表页，并打通 Maxwell→ES 链路。
--
-- 拍板依据（用户原话，2026-10-03）：
--   "020走B,生产库有隐私内容，不适合导入作为测试"
--   → 不走 A（导入生产 dump），改为生成种子内容；**本脚本内容全部虚构，零真实隐私数据**。
--
-- 幂等：数据使用 `id >= 90001` 的保留段 + `INSERT IGNORE` + 固定时间戳，可重复执行。
--       重复执行不产生新行、不改变既有数据。
--
-- 回滚：执行同目录 `content_seed_cleanup.sql`（仅删 id >= 90001 的行，不碰既有数据）。
--       ⚠️ 注意：**不要**用 git 快照 tag 当 DB 回滚依据——`工具/snapshot.ps1` 是纯
--       git 工作树快照，不备份数据库（020 风险节口径更正，2026-10-03）。
--
-- 覆盖范围：
--   t_category(3) / t_tag(8) / t_article(6) / t_article_tag / t_talk(3)
--   / t_photo_album(2) / t_photo(4)
-- 关联设计（刻意覆盖多分支）：
--   - 2 篇 is_top=1 → 首页置顶卡（含 ISS-014 曾修的 topArticle 路径）
--   - 2 篇 is_featured=1 → 首页精选区
--   - 1 篇 status=2 → 列表口径 `status in (1,2)` 的边界分支
--   - 1 篇含 KaTeX 公式 → 联动队列项 021 的 KaTeX vendoring 走查
--   - 1 篇无封面 → 走 default-card-cover 回落分支
-- =============================================================

SET NAMES utf8mb4;

-- ---------- 分类 ----------
INSERT IGNORE INTO `t_category` (`id`, `category_name`, `create_time`, `update_time`) VALUES
  (90001, '生活随笔', '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90002, '技术笔记', '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90003, '读书观影', '2026-09-01 10:00:00', '2026-09-01 10:00:00');

-- ---------- 标签 ----------
INSERT IGNORE INTO `t_tag` (`id`, `tag_name`, `create_time`, `update_time`) VALUES
  (90001, '随笔',   '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90002, 'Java',   '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90003, 'Vue',    '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90004, '算法',   '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90005, '日记',   '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90006, '数学',   '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90007, '影评',   '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90008, '随笔集', '2026-09-01 10:00:00', '2026-09-01 10:00:00');

-- ---------- 说说（碎片） ----------
INSERT IGNORE INTO `t_talk` (`id`, `user_id`, `content`, `images`, `is_top`, `status`, `create_time`, `update_time`) VALUES
  (90001, 1, '把博客的内容数据补齐了，首页终于不是空的。', NULL, 1, 1, '2026-09-28 20:15:00', '2026-09-28 20:15:00'),
  (90002, 1, '今天把前端的外域 CDN 依赖换成了本地 vendor，首屏不再等几十秒。', NULL, 0, 1, '2026-09-29 21:30:00', '2026-09-29 21:30:00'),
  (90003, 1, '测试一条说说，用来验证碎片页的渲染。', NULL, 0, 1, '2026-09-30 09:05:00', '2026-09-30 09:05:00');

-- ---------- 相册 ----------
INSERT IGNORE INTO `t_photo_album` (`id`, `album_name`, `album_desc`, `album_cover`, `is_delete`, `status`, `create_time`, `update_time`) VALUES
  (90001, '示例相册', '用于本地验证的示例相册', '/img/seed/cover-1.png', 0, 1, '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90002, '风景',     '示例风景相册',       '/img/seed/cover-2.png', 0, 1, '2026-09-01 10:00:00', '2026-09-01 10:00:00');

-- ---------- 照片 ----------
INSERT IGNORE INTO `t_photo` (`id`, `album_id`, `photo_name`, `photo_desc`, `photo_src`, `is_delete`, `create_time`, `update_time`) VALUES
  (90001, 90001, '示例图一', '种子示例图', '/img/seed/cover-1.png', 0, '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90002, 90001, '示例图二', '种子示例图', '/img/seed/cover-2.png', 0, '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90003, 90002, '示例图三', '种子示例图', '/img/seed/cover-3.png', 0, '2026-09-01 10:00:00', '2026-09-01 10:00:00'),
  (90004, 90002, '示例图四', '种子示例图', '/img/seed/cover-4.png', 0, '2026-09-01 10:00:00', '2026-09-01 10:00:00');

-- ---------- 文章 ----------
-- ⚠️ 内容格式 = **Markdown**，不是 HTML。
--    本仓文章详情/列表走 markdown-it 渲染（`aurora-blog/src/utils/markdown.ts`：
--    markdown-it + markdown-it-katex-external + markdown-it-emoji）。
--    若存 HTML，会被 markdown-it 当作**纯文本**渲染，页面显示出 `&lt;h2&gt;` 这类转义标签。
--    公式定界符：行内 `$...$`、块级 `$$...$$`。
--    注意 MySQL 字符串里反斜杠是转义符，故 LaTeX 命令需写双反斜杠（`\\sum` → 实际 `\sum`）。
--
-- 口径：is_delete=0；status 1(公开)/2(私密，列表口径含 2)；type=1(原创)；
--       user_id=1（本地唯一用户 admin@163.com，非真实隐私数据）。
INSERT IGNORE INTO `t_article`
  (`id`, `user_id`, `category_id`, `article_cover`, `article_title`, `article_content`,
   `is_top`, `is_featured`, `is_delete`, `status`, `type`, `password`, `original_url`,
   `create_time`, `update_time`)
VALUES
  -- 1. 置顶 + 精选：覆盖标题/列表/引用/代码块/行内公式等常见排版
  (90001, 1, 90001, '/img/seed/cover-1.png', '欢迎来到这片小小的星尘',
   '## 你好，星尘\n\n这是本地环境的**种子文章**，用于恢复前台的展示链路。\n\n它覆盖了几种常见的排版元素：\n\n- 无序列表\n- 引用\n- 代码块\n- 行内公式\n\n> 你我都是小小的星尘。\n\n```java\npublic class Hello {\n    public static void main(String[] args) {\n        System.out.println("Hello, Stardust");\n    }\n}\n```\n\n用一句公式收尾：$E = mc^2$\n\n如果上面的代码块和公式都正常排版，说明文章详情页的 markdown 渲染链路是通的。',
   1, 1, 0, 1, 1, NULL, NULL, '2026-09-25 09:00:00', '2026-09-25 09:00:00'),

  -- 2. 置顶：走 topArticle 路径（ISS-014 修过的置顶卡）
  (90002, 1, 90002, '/img/seed/cover-2.png', '这次把首屏从几十秒拉回了半秒',
   '## 问题\n\n首页 `index.html` 里有两个阻塞脚本指向不可达的 CDN，浏览器解析器卡在 `head`，Vue 挂载被推迟到几十秒之后。\n\n## 处置\n\n把 jquery 与 KaTeX 的 dist 本地化到 `public/vendor/`，只替换 URL、保持行序不变。\n\n## 结果\n\n挂载耗时从 **16~56 秒** 降到 **数百毫秒**，失败加载从 5 条降到 0 条。\n\n一句话总结：**阻塞 head 的外域脚本，比缺失装饰危险得多。**',
   1, 0, 0, 1, 1, NULL, NULL, '2026-09-26 14:30:00', '2026-09-26 14:30:00'),

  -- 3. 精选 + 公式：联动队列项 021 的 KaTeX vendoring 走查
  (90003, 1, 90002, '/img/seed/cover-3.png', '一条公式的排版演进',
   '## 背景\n\nKaTeX 一直用来渲染文章里的数学公式。这个版本的样式表里带着一处行高处理，会让公式所在行的行高略紧。\n\n## 一个例子\n\n质能方程：$E = mc^2$\n\n求和公式：\n\n$$\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}$$\n\n矩阵：\n\n$$\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}$$\n\n## 小结\n\n如果上面几处公式都渲染成了正常排版而非原始文本，说明 KaTeX 的脚本与字体都已从本地正确加载。',
   0, 1, 0, 1, 1, NULL, NULL, '2026-09-27 11:20:00', '2026-09-27 11:20:00'),

  -- 4. 精选 + 无封面：走 default-card-cover 回落分支
  (90004, 1, 90003, NULL, '读《小王子》的一点杂想',
   '## 关于驯养\n\n书里最打动人的，大概是“驯养”这个说法：你为你的玫瑰花费了时间，才使你的玫瑰变得重要。\n\n---\n\n这篇故意**不设封面图**，用来验证无封面时列表卡片会回落到默认封面，而不是渲染出裂图。',
   0, 1, 0, 1, 1, NULL, NULL, '2026-09-28 19:45:00', '2026-09-28 19:45:00'),

  -- 5. status=2：覆盖列表口径 status in (1,2) 的边界分支
  (90005, 1, 90001, '/img/seed/cover-5.png', '一篇设了访问状态的笔记',
   '## 说明\n\n这篇文章的 `status` 设为 2。列表查询的口径是 `status in (1,2)`，所以它应当**仍然出现在列表里**。\n\n它用来验证列表口径没有把状态为 2 的内容漏掉。',
   0, 0, 0, 2, 1, NULL, NULL, '2026-09-29 08:10:00', '2026-09-29 08:10:00'),

  -- 6. 兜底一篇普通文章，补齐分页第二页
  (90006, 1, 90002, '/img/seed/cover-6.png', '分页与骨架屏的走查样本',
   '## 用途\n\n多凑一篇，让列表长度越过第一页的阈值，方便验证分页组件与骨架屏在翻页时的表现。\n\n也顺便验证侧栏的“最新文章”“标签云”等区块在有数据时的排布。',
   0, 0, 0, 1, 1, NULL, NULL, '2026-09-30 16:00:00', '2026-09-30 16:00:00');

-- ---------- 文章-标签 关联 ----------
INSERT IGNORE INTO `t_article_tag` (`id`, `article_id`, `tag_id`) VALUES
  (90001, 90001, 90001), (90002, 90001, 90005), (90003, 90001, 90008),
  (90004, 90002, 90003), (90005, 90002, 90001),
  (90006, 90003, 90006), (90007, 90003, 90005),
  (90008, 90004, 90007), (90009, 90004, 90001),
  (90010, 90005, 90001), (90011, 90005, 90005),
  (90012, 90006, 90002), (90013, 90006, 90004);

-- ---------- 校验 ----------
SELECT 't_category' AS tbl, COUNT(*) AS seeded FROM t_category WHERE id >= 90001
UNION ALL SELECT 't_tag', COUNT(*) FROM t_tag WHERE id >= 90001
UNION ALL SELECT 't_article', COUNT(*) FROM t_article WHERE id >= 90001
UNION ALL SELECT 't_article_tag', COUNT(*) FROM t_article_tag WHERE id >= 90001
UNION ALL SELECT 't_talk', COUNT(*) FROM t_talk WHERE id >= 90001
UNION ALL SELECT 't_photo_album', COUNT(*) FROM t_photo_album WHERE id >= 90001
UNION ALL SELECT 't_photo', COUNT(*) FROM t_photo WHERE id >= 90001
UNION ALL SELECT 't_article(列表口径)', COUNT(*) FROM t_article WHERE is_delete = 0 AND status IN (1,2);
