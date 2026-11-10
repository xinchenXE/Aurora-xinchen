package com.aurora.security;

import java.util.List;

/**
 * 显式匿名路由白名单（队列项 007 拍板：方案 A+C）。
 *
 * <h2>为什么需要它</h2>
 * {@code ResourceRoleAuthorizationManager} 的语义是「**未匹配到受管资源即放行**」——
 * 也就是说，一条路由只要**没登记进 {@code t_resource}**，它就自动对匿名开放。
 * 这让"新增/改名路由"变成一次**静默的放行面扩大**：没有编译错误、没有测试红灯、
 * 没有 CI 拦截，直到有人在生产上发现某个内部接口能被未登录用户调用。
 * 历史 P0 漏因（{@code ISS-020}：两个 admin 上传接口匿名可达）就是这么来的。
 *
 * <h2>这个类的作用</h2>
 * 把「**有意匿名**」显式写下来。配套单测 {@code RouteResourceRegistrationTest}
 * 会把「全部 {@code @RestController} 路由」与「{@code t_resource} 登记 + 本白名单」
 * 做差集断言——**差集不为空就红灯**，从而把上面那次静默扩大变成一次构建失败。
 *
 * <h2>维护规则</h2>
 * <ul>
 *   <li>新增一条**有意匿名**的路由 → 在此加一行，并写明匿名理由；</li>
 *   <li>新增一条**应受保护**的路由 → 登记进 {@code t_resource}（含 {@code t_role_resource}），
 *       **不要**加到这里；</li>
 *   <li>删除/改名路由 → 同步删掉此处的对应行，否则单测会因"白名单里有一条不存在的路由"而失败。</li>
 * </ul>
 *
 * <p>口径来源：{@code 活跃计划/2026-09-30-007-route-resource-registration-guard-test.md}
 * 的「合并拍板材料」节（2026-09-30 用户裁决："均授权你做…按推荐来"）。
 */
public final class AnonymousRouteWhitelist {

    /**
     * 一条有意匿名的路由。
     *
     * @param method HTTP 方法（大写）
     * @param path   路径模板，与 Spring 的写法一致（如 {@code /comments/{commentId}/replies}）
     * @param reason 匿名理由 —— **必填**，目的是让后来者能判断"这条还算不算有意"
     */
    public record AnonymousRoute(String method, String path, String reason) {
    }

    /**
     * 当前全部有意匿名的路由。
     * <p>
     * 注意：**这里不包含**已登记进 {@code t_resource} 的匿名资源
     * （例如 {@code /articles/*}，它们在库里 {@code is_anonymous=1} 已有登记），
     * 本清单只覆盖「**连登记都没有**、但确实应当匿名」的那几条。
     */
    public static final List<AnonymousRoute> ROUTES = List.of(
            new AnonymousRoute("GET", "/ai/assistant",
                    "只返回展示用的角色名与开场白，不含任何用户数据；AGENTS.md 四节 6 明文规定匿名可用"),

            new AnonymousRoute("POST", "/articles/access",
                    "校验文章的访问密码。文章正文本身已匿名可读（/articles/* 在库中 is_anonymous=1），"
                            + "校验动作必须发生在登录之前，否则受保护文章对未登录访客完全不可达"),

            new AnonymousRoute("GET", "/comments/{commentId}/replies",
                    "查看某条评论的回复列表。同级 GET /comments 已登记为匿名，评论内容本就公开可读；"
                            + "若改为需登录，未登录访客点开评论会得到 40001，属体验回退"),

            new AnonymousRoute("GET", "/comments/topSix",
                    "首页展示的前六条评论，与上一条同源。"
                            + "另注：库中登记的是 /comments/topSeven（代码里已无此接口），属死登记，待清理"),

            new AnonymousRoute("GET", "/remote/article/all",
                    "聚合第三方博客源的公开文章列表，供访客浏览，无需登录。"
                            + "⚠️ 其入参 targetSource 为客户端可控字符串（只用于在已注册源里查表，不拼 SQL），"
                            + "若要收敛为受限取值，应另立队列项")
    );

    private AnonymousRouteWhitelist() {
    }

    /**
     * 判断某条路由是否在白名单内（方法名 + 路径**完全一致**才算命中）。
     */
    public static boolean contains(String method, String path) {
        return ROUTES.stream()
                .anyMatch(r -> r.method().equalsIgnoreCase(method) && r.path().equals(path));
    }
}
