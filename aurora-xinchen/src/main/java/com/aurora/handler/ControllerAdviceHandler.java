package com.aurora.handler;

import com.aurora.aspect.ExceptionLogAspect;
import com.aurora.model.vo.ResultVO;
import com.aurora.enums.StatusCodeEnum;
import com.aurora.exception.BizException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;


@Log4j2
@RestControllerAdvice
public class ControllerAdviceHandler {

    @Autowired
    private ExceptionLogAspect exceptionLogAspect;

    @ExceptionHandler(value = BizException.class)
    public ResultVO<?> errorHandler(BizException e) {
        return ResultVO.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVO<?> errorHandler(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = StatusCodeEnum.VALID_ERROR.getDesc();
        if (Objects.nonNull(e.getBindingResult().getFieldError())) {
            message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        }
        log.error("参数校验异常: uri={}, message={}", request.getRequestURI(), message, e);
        saveExceptionLog(request, e);
        return ResultVO.fail(StatusCodeEnum.VALID_ERROR.getCode(), message);
    }

    @ExceptionHandler(value = Exception.class)
    public ResultVO<?> errorHandler(Exception e, HttpServletRequest request) {
        log.error("系统异常: uri={}", request.getRequestURI(), e);
        saveExceptionLog(request, e);
        return ResultVO.fail(StatusCodeEnum.SYSTEM_ERROR.getCode(), StatusCodeEnum.SYSTEM_ERROR.getDesc());
    }

    /**
     * 补记异常日志。
     * <p>
     * 控制器方法内部抛出的异常已由 {@link ExceptionLogAspect} 的 @AfterThrowing 落库，
     * 这里通过请求属性去重；参数解析/绑定阶段的异常发生在方法调用之前、切面切不到，
     * 在此补记，保证响应体、文件日志、t_exception_log 三处口径一致。
     */
    private void saveExceptionLog(HttpServletRequest request, Exception e) {
        if (Objects.isNull(request)
                || Boolean.TRUE.equals(request.getAttribute(ExceptionLogAspect.EXCEPTION_RECORDED_ATTRIBUTE))) {
            return;
        }
        exceptionLogAspect.record(request, resolveOptMethod(e), null, "", e);
    }

    /**
     * 从参数绑定类异常中解析出目标控制器方法名，格式与切面一致（全限定类名.方法名）；
     * 无法解析时返回空串，不阻断主流程。
     */
    private String resolveOptMethod(Exception e) {
        MethodParameter parameter = null;
        if (e instanceof MethodArgumentNotValidException validException) {
            parameter = validException.getParameter();
        } else if (e instanceof MethodArgumentTypeMismatchException mismatchException) {
            parameter = mismatchException.getParameter();
        }
        if (Objects.isNull(parameter) || Objects.isNull(parameter.getExecutable())) {
            return "";
        }
        return parameter.getExecutable().getDeclaringClass().getName() + "." + parameter.getExecutable().getName();
    }

}
