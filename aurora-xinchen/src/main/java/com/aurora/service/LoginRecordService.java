package com.aurora.service;

import com.aurora.model.dto.UserDetailsDTO;

/**
 * 登录记录（IP / 地区 / 最近登录时间）写入。
 * <p>
 * 队列项 029：原先 {@code AuthenticationSuccessHandlerImpl#updateUserInfo()} 标注了 {@code @Async}
 * 却在同一个 bean 内被直接调用，Spring 代理不生效 → 实际同步执行在请求线程上；
 * 且该方法依赖 {@code UserUtil}（ThreadLocal），一旦真的异步化就会 NPE。
 * 现拆为独立 bean：由 {@code AuthenticationSuccessHandlerImpl} 通过代理调用，
 * 参数显式传入，任何异常都只记日志、不影响登录响应。
 */
public interface LoginRecordService {

    /**
     * 异步更新最近登录信息。调用方不在请求线程上依赖 SecurityContext。
     *
     * @param userDetailsDTO 从 authentication.getPrincipal() 取得的快照
     */
    void recordLogin(UserDetailsDTO userDetailsDTO);
}
