package com.aurora.handler;

import com.aurora.mapper.RoleMapper;
import com.aurora.model.dto.ResourceRoleDTO;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 资源-角色授权管理器（Security 6）
 * 替代旧版 FilterInvocationSecurityMetadataSource + AccessDecisionManager 组合：
 * 按数据库中的资源-角色映射匹配当前请求，直接给出授权决定。
 */
@Component
public class ResourceRoleAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Autowired
    private RoleMapper roleMapper;

    private static List<ResourceRoleDTO> resourceRoleList;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @PostConstruct
    private void loadResourceRoleList() {
        resourceRoleList = roleMapper.listResourceRoles();
    }

    /**
     * 资源-角色映射变更后清空缓存，下次请求时重新加载
     */
    public void clearDataSource() {
        resourceRoleList = null;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        if (CollectionUtils.isEmpty(resourceRoleList)) {
            this.loadResourceRoleList();
        }
        HttpServletRequest request = context.getRequest();
        String method = request.getMethod();
        String url = request.getRequestURI();
        for (ResourceRoleDTO resourceRoleDTO : resourceRoleList) {
            if (antPathMatcher.match(resourceRoleDTO.getUrl(), url) && resourceRoleDTO.getRequestMethod().equals(method)) {
                List<String> roleList = resourceRoleDTO.getRoleList();
                // 资源未配置任何角色：视为禁用，拒绝所有访问
                if (CollectionUtils.isEmpty(roleList)) {
                    return new AuthorizationDecision(false);
                }
                Authentication auth = authentication.get();
                if (auth == null) {
                    return new AuthorizationDecision(false);
                }
                Set<String> authorities = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet());
                for (String role : roleList) {
                    if (authorities.contains(role)) {
                        return new AuthorizationDecision(true);
                    }
                }
                return new AuthorizationDecision(false);
            }
        }
        // 未匹配到受管资源：放行（与原实现 getAttributes 返回 null 时跳过拦截一致）
        return new AuthorizationDecision(true);
    }
}
