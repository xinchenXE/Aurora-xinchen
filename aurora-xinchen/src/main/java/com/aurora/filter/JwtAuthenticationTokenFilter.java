package com.aurora.filter;


import com.aurora.model.dto.UserDetailsDTO;
import com.aurora.service.TokenService;
import com.aurora.util.UserUtil;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * JWT 认证过滤器。
 * <p>
 * 队列项 029：原实现标注 {@code @SneakyThrows}，过期 / 被篡改 / 格式错误的历史 token 会让
 * {@code parseToken} 抛出 {@code JwtException}，直接冒到容器形成裸 HTTP 500——
 * **任意接口（包括登录）** 只要带着旧 token 就会 500。
 * 现在：JwtException 一律按「未认证」处理（清空上下文、继续走后续过滤器），
 * 由后续的匿名/鉴权规则给出 40001，而不是 500。
 */
@Slf4j
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    @Autowired
    public TokenService tokenService;

    @Autowired
    public AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws java.io.IOException, jakarta.servlet.ServletException {
        try {
            UserDetailsDTO userDetailsDTO = tokenService.getUserDetailDTO(request);
            if (Objects.nonNull(userDetailsDTO) && Objects.isNull(UserUtil.getAuthentication())) {
                tokenService.renewToken(userDetailsDTO);
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetailsDTO, null, userDetailsDTO.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (JwtException | IllegalArgumentException e) {
            // 过期 / 篡改 / 非 Base64 的 token：按未登录处理，不中断请求
            log.warn("忽略无效的 Authorization token: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
