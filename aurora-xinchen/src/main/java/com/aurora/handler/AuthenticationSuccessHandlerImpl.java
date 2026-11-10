package com.aurora.handler;

import com.alibaba.fastjson2.JSON;
import com.aurora.constant.CommonConstant;
import com.aurora.enums.StatusCodeEnum;
import com.aurora.model.dto.UserDetailsDTO;
import com.aurora.model.dto.UserInfoDTO;
import com.aurora.model.vo.ResultVO;
import com.aurora.service.LoginRecordService;
import com.aurora.service.TokenService;
import com.aurora.util.BeanCopyUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 登录成功处理器。
 * <p>
 * 队列项 029：本方法内的异常**不属于** {@code AuthenticationException}，
 * Spring Security 不会转给 {@code AuthenticationFailureHandler}，而是直接冒到容器形成裸 HTTP 500；
 * {@code ControllerAdviceHandler} 也管不到过滤器链。实测根因是 {@code tokenService.createToken()}
 * 在密钥为空 / 非法 Base64 / 解码不足 32 字节时抛异常（本地已 1:1 复现）。
 * <p>
 * 现在：整段包 try/catch，把 JWT / Redis 故障转成结构化错误（HTTP 200 + 50000），
 * 同时把**完整堆栈**写进日志（这一段日志是排障的唯一线索，不得省略）。
 */
@Slf4j
@Component
public class AuthenticationSuccessHandlerImpl implements AuthenticationSuccessHandler {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginRecordService loginRecordService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        response.setContentType(CommonConstant.APPLICATION_JSON);
        try {
            UserDetailsDTO userDetailsDTO = (UserDetailsDTO) authentication.getPrincipal();
            UserInfoDTO userLoginDTO = BeanCopyUtil.copyObject(userDetailsDTO, UserInfoDTO.class);
            String token = tokenService.createToken(userDetailsDTO);
            userLoginDTO.setToken(token);
            response.getWriter().write(JSON.toJSONString(ResultVO.ok(userLoginDTO)));
            // 独立 bean 调用 → @Async 真正生效；参数显式传递，不依赖 SecurityContext
            loginRecordService.recordLogin(userDetailsDTO);
        } catch (Exception e) {
            // 凭据已校验通过，失败发生在签发令牌 / 写缓存阶段
            log.error("登录成功但签发令牌失败（请检查 auth.secret 与 Redis 配置）", e);
            if (!response.isCommitted()) {
                response.getWriter().write(JSON.toJSONString(ResultVO.fail(StatusCodeEnum.SYSTEM_ERROR)));
            }
        }
    }
}
