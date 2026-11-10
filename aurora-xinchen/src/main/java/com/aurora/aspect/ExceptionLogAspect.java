package com.aurora.aspect;

import com.alibaba.fastjson2.JSON;
import com.aurora.entity.ExceptionLog;
import com.aurora.event.ExceptionLogEvent;
import com.aurora.util.ExceptionUtil;
import com.aurora.util.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Objects;

@Aspect
@Component
public class ExceptionLogAspect {

    /**
     * 请求属性名：标记该请求的异常已由本切面落库。
     * 全局兜底分支（ControllerAdviceHandler）据此去重，避免同一异常重复写 t_exception_log。
     */
    public static final String EXCEPTION_RECORDED_ATTRIBUTE = "AURORA_EXCEPTION_RECORDED";

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 异常落库切点：覆盖全部 REST 控制器所在包。
     * <p>
     * 2026-10-02（队列项 018 / ISS-021）：原为 {@code execution(* com.aurora.controller..*.*(..))}，
     * 漏掉 {@code com.aurora.remote}（{@code RemoteArticleController} 在该包），
     * 导致其异常只能走 {@code ControllerAdviceHandler} 兜底补记、{@code opt_method} 落库为空。
     * 兜底补记的字段完整度低于本切面（无方法名、无入参、无 opt_desc），故补全该包。
     * <p>
     * 不采用 {@code @within(@RestController)} 注解切的备选修法：本切面用
     * {@code method.getAnnotation(Operation.class)} 取 {@code opt_desc}，
     * 而 {@code @Operation} 声明在接口方法上；改注解切后由实现类方法取注解可能取空，
     * 会使 {@code opt_desc} 静默丢失（详见 018 代码修改记录）。新增 REST 包时需在下方补包。
     */
    @Pointcut("execution(* com.aurora.controller..*.*(..)) || execution(* com.aurora.remote..*.*(..))")
    public void exceptionLogPointcut() {
    }

    @AfterThrowing(value = "exceptionLogPointcut()", throwing = "e")
    public void saveExceptionLog(JoinPoint joinPoint, Exception e) {
        HttpServletRequest request = currentRequest();
        if (Objects.isNull(request)) {
            return;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = className + "." + method.getName();
        String requestParam = null;
        if (joinPoint.getArgs().length > 0) {
            if (joinPoint.getArgs()[0] instanceof MultipartFile) {
                requestParam = "file";
            } else {
                requestParam = JSON.toJSONString(joinPoint.getArgs());
            }
        }
        Operation apiOperation = method.getAnnotation(Operation.class);
        String optDesc = Objects.nonNull(apiOperation) ? apiOperation.summary() : "";
        record(request, methodName, requestParam, optDesc, e);
    }

    /**
     * 构建异常日志并发布落库事件（由 AuroraListener 异步写入 t_exception_log）。
     * <p>
     * 参数解析/绑定阶段的异常发生在控制器方法调用之前，切点切不到，
     * 由 ControllerAdviceHandler 复用本方法补记，字段口径与切面保持一致。
     */
    public void record(HttpServletRequest request, String optMethod, String requestParam, String optDesc, Exception e) {
        ExceptionLog exceptionLog = new ExceptionLog();
        exceptionLog.setOptUri(request.getRequestURI());
        exceptionLog.setOptMethod(optMethod);
        exceptionLog.setRequestMethod(request.getMethod());
        exceptionLog.setRequestParam(requestParam);
        exceptionLog.setOptDesc(optDesc);
        exceptionLog.setExceptionInfo(ExceptionUtil.getTrace(e));
        String ipAddress = IpUtil.getIpAddress(request);
        exceptionLog.setIpAddress(ipAddress);
        exceptionLog.setIpSource(IpUtil.getIpSource(ipAddress));
        request.setAttribute(EXCEPTION_RECORDED_ATTRIBUTE, Boolean.TRUE);
        applicationContext.publishEvent(new ExceptionLogEvent(exceptionLog));
    }

    /**
     * 取当前请求上下文中的 HttpServletRequest；非 Web 线程（如异步任务）返回 null。
     */
    public static HttpServletRequest currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (Objects.isNull(requestAttributes)) {
            return null;
        }
        return (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
    }

}
