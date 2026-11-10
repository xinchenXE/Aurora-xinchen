package com.aurora.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 队列项 007：**路由 ↔ t_resource 登记** 的回归护栏。
 *
 * <h2>它防什么</h2>
 * {@code ResourceRoleAuthorizationManager} 对「未登记」的路由**一律放行**。
 * 于是"新增一条 Controller 路由" = "给匿名用户开一个口子"，
 * 而这个事实**没有任何编译期或测试期的信号**。历史上 {@code ISS-020}
 * （两个 admin 上传接口匿名可达）就是这么漏的。
 *
 * <h2>它怎么防</h2>
 * 反射扫描全部含 {@code @RestController} 的包 → 得到路由全集；
 * 与「种子 SQL 里 {@code t_resource} 已登记的 url + {@link AnonymousRouteWhitelist}」求差集；
 * **差集非空即红灯**，并在断言消息里列出是哪几条、该走哪条路（登记 or 加白名单）。
 *
 * <h2>设计取舍</h2>
 * <ul>
 *   <li>**不启动 Spring 上下文**，纯反射 + 纯文本解析，避免拖慢测试；</li>
 *   <li>种子 SQL 以**解析文本**的方式读取，不连数据库——让本测试在 CI 上也能跑；</li>
 *   <li>正因此，它校验的是「**种子 SQL 与代码是否一致**」，
 *       而非「生产库与代码是否一致」。数据库侧的漂移需另由运维巡检覆盖。</li>
 * </ul>
 *
 * @see AnonymousRouteWhitelist
 */
class RouteResourceRegistrationTest {

    /** 含 @RestController 的包（新增 REST 包时必须同步这里，否则会出现扫描盲区） */
    private static final List<String> REST_PACKAGES = List.of(
            "src/main/java/com/aurora/controller",
            "src/main/java/com/aurora/remote"
    );

    /** 种子 SQL：以初始化脚本为准（内层 sql/aurora.sql 已是旧版，缺 t_ai_persona） */
    private static final String SEED_SQL = "../setup/sql/aurora-init.sql";

    private static final Pattern CLASS_ANNO =
            Pattern.compile("@RestController\\b");
    private static final Pattern CLASS_ANNO_ADVICE =
            Pattern.compile("@RestControllerAdvice\\b");
    private static final Pattern CLASS_BASE =
            Pattern.compile("@RequestMapping\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"");
    private static final Pattern METHOD_MAPPING = Pattern.compile(
            "@(Get|Post|Put|Delete|Patch)Mapping\\s*(?:\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\")?");

    // ------------------------------------------------------------------
    // 路由扫描
    // ------------------------------------------------------------------

    private record Route(String method, String path, String simpleClass) {
    }

    private static List<Route> scanRoutes() throws IOException {
        List<Route> routes = new ArrayList<>();
        for (String pkg : REST_PACKAGES) {
            Path dir = Paths.get(pkg);
            assertTrue(Files.isDirectory(dir),
                    "扫描目录不存在：" + dir.toAbsolutePath()
                            + "（路径相对于模块目录）");
            try (Stream<Path> files = Files.list(dir)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    routes.addAll(scanFile(f));
                }
            }
        }
        return routes;
    }

    private static List<Route> scanFile(Path f) throws IOException {
        String src = Files.readString(f, StandardCharsets.UTF_8);
        // 排除 @RestControllerAdvice（异常处理器不是路由）
        if (!CLASS_ANNO.matcher(src).find() || CLASS_ANNO_ADVICE.matcher(src).find()) {
            return List.of();
        }
        // 去掉注释，避免注释里的 @GetMapping 被误扫
        String body = stripComments(src);

        Matcher bm = CLASS_BASE.matcher(body);
        String base = bm.find() ? bm.group(1) : "";

        String simple = f.getFileName().toString().replace(".java", "");
        List<Route> out = new ArrayList<>();
        Matcher mm = METHOD_MAPPING.matcher(body);
        while (mm.find()) {
            String method = mm.group(1).toUpperCase();
            String sub = mm.group(2) == null ? "" : mm.group(2);
            String path = (base + sub).replaceAll("/{2,}", "/");
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            out.add(new Route(method, path, simple));
        }
        return out;
    }

    /** 粗略剥离 // 行注释与 block 注释（够用即可：本项目映射注解不写在注释里） */
    private static String stripComments(String s) {
        return s.replaceAll("/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    // ------------------------------------------------------------------
    // 种子 SQL 登记集合
    // ------------------------------------------------------------------

    /** 从种子 SQL 的 t_resource INSERT 中提取 (method, url) */
    private static Set<String> registeredFromSeedSql() throws IOException {
        Path sql = Paths.get(SEED_SQL);
        assertTrue(Files.exists(sql), "种子 SQL 不存在：" + sql.toAbsolutePath());
        String s = Files.readString(sql, StandardCharsets.UTF_8);

        Matcher m = Pattern.compile(
                "INSERT INTO `t_resource` VALUES\\s*(.*?);\\s*\\n", Pattern.DOTALL).matcher(s);
        assertTrue(m.find(), "在 " + SEED_SQL + " 中未找到 t_resource 的 INSERT 语句");

        String body = m.group(1);
        // 逐元组解析：id,'name',url,method,parent,is_anonymous,create,update
        Pattern tuple = Pattern.compile(
                "\\((\\d+),'(?:[^']|'')*',(NULL|'(?:[^']|'')*'),(NULL|'(?:[^']|'')*'),[^,]+,[^,]+,[^,]+,[^)]*\\)");
        Matcher tm = tuple.matcher(body);
        Set<String> out = new LinkedHashSet<>();
        while (tm.find()) {
            String url = unq(tm.group(2));
            String method = unq(tm.group(3));
            if (url == null || method == null) {
                continue; // 模块父项（url/method 为 NULL）
            }
            out.add(method.toUpperCase() + " " + url);
        }
        assertTrue(out.size() > 100, "解析出的登记条数异常偏少（" + out.size() + "），解析逻辑可能失效");
        return out;
    }

    private static String unq(String v) {
        if (v == null || "NULL".equalsIgnoreCase(v)) {
            return null;
        }
        return v.startsWith("'") && v.endsWith("'") ? v.substring(1, v.length() - 1) : v;
    }

    // ------------------------------------------------------------------
    // 匹配
    // ------------------------------------------------------------------

    /** Ant 语义：{@code *} 匹配段内任意字符（本项目 t_resource 的 url 用法） */
    private static boolean antMatch(String pattern, String actual) {
        String re = Stream.of(pattern.split("/", -1))
                .map(seg -> Stream.of(seg.split("\\*", -1))
                        .map(Pattern::quote)
                        .collect(Collectors.joining("[^/]*")))
                .collect(Collectors.joining("/"));
        return actual.matches(re);
    }

    // ------------------------------------------------------------------
    // 断言
    // ------------------------------------------------------------------

    @Test
    void everyRestControllerRouteIsEitherRegisteredOrExplicitlyAnonymous() throws Exception {
        List<Route> routes = scanRoutes();
        assertTrue(routes.size() > 100, "路由扫描条数异常偏少（" + routes.size() + "）");

        Set<String> registered = registeredFromSeedSql();

        List<String> offenders = new ArrayList<>();
        for (Route r : routes) {
            boolean inSeed = registered.stream().anyMatch(entry -> {
                int i = entry.indexOf(' ');
                return entry.substring(0, i).equals(r.method())
                        && antMatch(entry.substring(i + 1), r.path());
            });
            if (inSeed) {
                continue;
            }
            if (AnonymousRouteWhitelist.contains(r.method(), r.path())) {
                continue;
            }
            offenders.add(r.method() + " " + r.path() + "   (" + r.simpleClass() + ")");
        }

        assertTrue(offenders.isEmpty(), buildFailureMessage(offenders, routes.size(), registered.size()));
    }

    private static String buildFailureMessage(List<String> offenders, int routeCount, int registeredCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("发现 ").append(offenders.size())
                .append(" 条路由既未登记进 t_resource，也不在匿名白名单中。\n")
                .append("按 ResourceRoleAuthorizationManager「未匹配即放行」的语义，")
                .append("这些路由**当前对匿名开放**。\n\n")
                .append("扫描路由 ").append(routeCount)
                .append(" 条；种子 SQL 已登记 ").append(registeredCount).append(" 条。\n\n")
                .append("请二选一：\n")
                .append("  (a) 该受保护 → 写幂等迁移脚本登记 t_resource + t_role_resource，")
                .append("并同步 ../setup/sql/aurora-init.sql；\n")
                .append("  (b) 确属有意匿名 → 加进 AnonymousRouteWhitelist，并写明理由。\n\n")
                .append("待处理：\n");
        offenders.forEach(o -> sb.append("  - ").append(o).append('\n'));
        return sb.toString();
    }

    @Test
    void whitelistEntriesActuallyExistAsRoutes() throws Exception {
        List<Route> routes = scanRoutes();
        List<String> stale = new ArrayList<>();
        for (AnonymousRouteWhitelist.AnonymousRoute w : AnonymousRouteWhitelist.ROUTES) {
            boolean found = routes.stream()
                    .anyMatch(r -> r.method().equalsIgnoreCase(w.method())
                            && samePathShape(r.path(), w.path()));
            if (!found) {
                stale.add(w.method() + " " + w.path());
            }
        }
        assertTrue(stale.isEmpty(),
                "匿名白名单里有 " + stale.size() + " 条路由在代码中已不存在，"
                        + "删除/改名时请同步清理 AnonymousRouteWhitelist：\n  - "
                        + String.join("\n  - ", stale));
    }

    /** 把 @PathVariable 的形式统一（{commentId} 与 {id} 视作同形），避免因改名误判为过期 */
    private static boolean samePathShape(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private static String normalize(String p) {
        return p.replaceAll("\\{[^}]*}", "{}");
    }

    @Test
    void whitelistEntriesAllCarryAReason() {
        for (AnonymousRouteWhitelist.AnonymousRoute w : AnonymousRouteWhitelist.ROUTES) {
            assertTrue(w.reason() != null && w.reason().length() >= 10,
                    "白名单条目缺少像样的匿名理由：" + w.method() + " " + w.path());
        }
    }
}
