package com.aurora.service.impl;

import com.aurora.entity.UserAuth;
import com.aurora.mapper.UserAuthMapper;
import com.aurora.model.dto.UserDetailsDTO;
import com.aurora.service.LoginRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 队列项 029：登录记录异步写入（独立 bean，确保 @Async 真正生效）。
 */
@Slf4j
@Service
public class LoginRecordServiceImpl implements LoginRecordService {

    /** 与 t_user_auth.ip_address / ip_source 的 varchar(50) 对齐，避免超长导致更新失败 */
    private static final int IP_COLUMN_LENGTH = 50;

    @Autowired
    private UserAuthMapper userAuthMapper;

    @Async
    @Override
    public void recordLogin(UserDetailsDTO userDetailsDTO) {
        if (userDetailsDTO == null || userDetailsDTO.getId() == null) {
            return;
        }
        try {
            UserAuth userAuth = UserAuth.builder()
                    .id(userDetailsDTO.getId())
                    .ipAddress(truncate(userDetailsDTO.getIpAddress()))
                    .ipSource(truncate(userDetailsDTO.getIpSource()))
                    .lastLoginTime(userDetailsDTO.getLastLoginTime())
                    .build();
            userAuthMapper.updateById(userAuth);
        } catch (Exception e) {
            // 登录已经成功，记录失败不允许影响用户
            log.error("更新最近登录信息失败 userId={}", userDetailsDTO.getId(), e);
        }
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= IP_COLUMN_LENGTH) {
            return value;
        }
        return value.substring(0, IP_COLUMN_LENGTH);
    }
}
