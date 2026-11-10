package com.aurora.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 队列项 001：AI 覆盖参数的**角色判定**单测。
 * <p>
 * 拍板口径：{@code model} 与 {@code rolePrompt} 均**仅 admin** 可用。
 * 本机库里只有 admin 一个用户（无 user 角色账号），
 * 故非 admin 分支无法走运行时实测 —— 用这里补足（见 001 代码修改记录第六节）。
 */
class AiChatControllerRoleCheckTest {

    private final AiChatController controller = new AiChatController();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateWith(String authority) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "principal", "credentials", List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void adminRoleIsRecognised() {
        authenticateWith("admin");
        assertTrue(controller.isAdmin(), "authority=admin 应判定为管理员");
    }

    @Test
    void nonAdminRolesAreRejected() {
        authenticateWith("user");
        assertFalse(controller.isAdmin(), "authority=user 不应判定为管理员");

        authenticateWith("test");
        assertFalse(controller.isAdmin(), "authority=test 不应判定为管理员");

        authenticateWith("guest");
        assertFalse(controller.isAdmin(), "未知 authority 不应判定为管理员");
    }

    @Test
    void unauthenticatedIsRejected() {
        SecurityContextHolder.clearContext();
        assertFalse(controller.isAdmin(), "无 SecurityContext 应为非管理员");
    }

    @Test
    void anonymousAuthenticationIsRejected() {
        // Spring Security 的匿名认证 isAuthenticated() 也是 true，必须靠 authority 区分
        Authentication anonymous = new UsernamePasswordAuthenticationToken(
                "anonymousUser", "N/A", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        assertFalse(controller.isAdmin(), "匿名认证不应判定为管理员");
    }
}
