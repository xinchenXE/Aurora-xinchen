package com.aurora.ai;

import com.aurora.config.properties.AiChatProperties;
import com.aurora.service.AiPersonaService;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI 对话服务（Spring AI 实现，替代原讯飞星火 Netty/WebSocket 接入）
 * 底层走 OpenAI 兼容协议，多模型兼容：通过 Nacos 配置 base-url/api-key/model
 * 可对接 OpenAI、DeepSeek、Kimi、智谱 GLM、Ollama、vLLM 等任意 OpenAI 协议端点，
 * 也可在单次请求中通过 model 参数临时切换模型（**受白名单约束**，见队列项 001）。
 * 人设（系统提示词）由后台「AI人设设置」模块维护（t_ai_persona，保存即生效），
 * 未配置时回退 Nacos 默认人设 aurora.ai.chat.system-prompt。
 */
@Log4j2
@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final AiPersonaService aiPersonaService;
    private final AiChatProperties aiChatProperties;

    public AiChatService(ChatClient.Builder chatClientBuilder, AiPersonaService aiPersonaService,
                         AiChatProperties aiChatProperties) {
        this.chatClient = chatClientBuilder.build();
        this.aiPersonaService = aiPersonaService;
        this.aiChatProperties = aiChatProperties;
    }

    /**
     * 同步对话：一次性返回完整回答。
     * rolePrompt 为空时使用默认人设；model 为空时使用配置的默认模型。
     */
    public String chat(String message, String rolePrompt, String model) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system(buildSystemPrompt(rolePrompt))
                .user(message);
        applyModelOverride(spec, model);
        return spec.call().content();
    }

    /**
     * 流式对话：逐片段推送回答（配合 SSE 使用）。
     * rolePrompt 为空时使用默认人设；model 为空时使用配置的默认模型。
     */
    public Flux<String> chatStream(String message, String rolePrompt, String model) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .system(buildSystemPrompt(rolePrompt))
                .user(message);
        applyModelOverride(spec, model);
        return spec.stream().content();
    }

    /**
     * 请求级模型覆盖：传入 model 时本次调用使用指定模型（多模型兼容）。
     * <p>
     * 队列项 001 拍板口径：取值必须在 Nacos 白名单 {@code aurora.ai.chat.allowed-models} 内，
     * **空列表 = 禁用覆盖**。不在白名单（含空列表）则忽略该参数，回落配置的默认模型。
     * 角色判断在上游控制器完成（仅 admin 能走到这里带非空 model）。
     */
    void applyModelOverride(ChatClient.ChatClientRequestSpec spec, String model) {
        if (model == null || model.isBlank()) {
            return;
        }
        String trimmed = model.trim();
        List<String> allowed = aiChatProperties.getAllowedModels();
        if (allowed == null || allowed.isEmpty() || !allowed.contains(trimmed)) {
            log.warn("拒绝模型覆盖：{} 不在白名单 aurora.ai.chat.allowed-models 内，回落默认模型", trimmed);
            return;
        }
        spec.options(OpenAiChatOptions.builder().model(trimmed).build());
    }

    /**
     * 组装系统提示词：基础人设（后台配置优先，Nacos 默认兜底）+ 可选的角色扮演要求
     */
    private String buildSystemPrompt(String rolePrompt) {
        String base = aiPersonaService.getEffectiveSystemPrompt();
        if (rolePrompt == null || rolePrompt.isBlank()) {
            return base;
        }
        return base + "\n\n# 角色扮演要求\n" + rolePrompt.trim()
                + "\n请在后续对话中始终保持上述角色设定进行回答。";
    }
}
