package com.aurora.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Base64;

/**
 * 异步线程池 + JWT 密钥启动期校验（队列项 029）。
 * <p>
 * 背景：`TokenServiceImpl.generalKey()` 在登录成功写响应之前解码 auth.secret，
 * 密钥为空 / 非法 Base64 / 解码不足 32 字节时抛异常，而成功处理器无 try/catch，
 * 异常会以裸 HTTP 500 的形式冒到容器（实测：本地把密钥换成模板占位符即 100% 复现）。
 * 这里把失败提前到启动期，并给出可直接照做的修复提示。
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    /** HS256 要求密钥至少 256 bit；jjwt 在不足时会抛 UnsupportedKeyException */
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${auth.secret:}")
    private String authSecret;

    @PostConstruct
    public void validateAuthSecret() {
        if (authSecret == null || authSecret.isBlank()) {
            throw new IllegalStateException(
                    "启动失败：auth.secret 未配置。请在 Nacos 配置中设置一个 Base64 编码、解码后至少 "
                            + MIN_SECRET_BYTES + " 字节的随机密钥，例如：openssl rand -base64 32");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(authSecret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "启动失败：auth.secret 不是合法的 Base64 字符串（当前值形如占位符或含非法字符）。"
                            + "请使用 openssl rand -base64 32 生成后写入 Nacos。", e);
        }
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "启动失败：auth.secret 解码后仅 " + decoded.length + " 字节，HS256 要求至少 "
                            + MIN_SECRET_BYTES + " 字节。请使用 openssl rand -base64 32 重新生成。");
        }
        log.info("auth.secret 校验通过（解码 {} 字节）", decoded.length);
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(20);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-task-thread-");
        return executor;
    }
}
