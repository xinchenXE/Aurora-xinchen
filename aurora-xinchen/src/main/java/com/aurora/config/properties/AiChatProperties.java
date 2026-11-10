package com.aurora.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话配置（Nacos 可热更新）
 * 配置前缀 aurora.ai.chat，示例见 nacos-config/application-cloud.yml
 */
@Data
@Component
@ConfigurationProperties(prefix = "aurora.ai.chat")
public class AiChatProperties {

    /**
     * 角色名称（默认人设名），后台未配置时使用
     */
    private String roleName = "爱莉希雅";

    /**
     * 开场白，后台未配置时使用
     */
    private String greeting = "嗨~ 想我了嘛🎶，爱莉希雅一直在这里哦~~~";

    /**
     * 基础系统提示词（默认人设），请求未携带角色扮演提示词时使用
     */
    private String systemPrompt = "你是 Aurora 博客的 AI 助手，回答简洁、准确、友好。";

    /**
     * 请求级模型覆盖的白名单（队列项 001 拍板）。
     * <p>
     * 语义：**仅 admin 角色**可传 {@code model}，且取值必须在此列表内；
     * **空列表 = 禁用模型覆盖**（非 admin 绕过且越权模型被拒时一律回落默认模型）。
     * 放 Nacos 是为了不发版即可调整。
     */
    private List<String> allowedModels = new ArrayList<>();

}
