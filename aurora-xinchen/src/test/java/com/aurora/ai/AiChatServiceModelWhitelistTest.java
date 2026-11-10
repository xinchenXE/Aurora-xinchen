package com.aurora.ai;

import com.aurora.config.properties.AiChatProperties;
import com.aurora.service.AiPersonaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 队列项 001：模型覆盖白名单的单测（不依赖 Nacos / LLM / DB）。
 * <p>
 * 拍板口径：仅 admin 可传 model（角色判断在控制器，这里只验白名单）；
 * 取值须在 {@code aurora.ai.chat.allowed-models} 内；**空列表 = 禁用覆盖**；
 * 非白名单一律**忽略**（回落默认模型），不抛错。
 */
class AiChatServiceModelWhitelistTest {

    private AiChatProperties properties;
    private AiChatService service;
    private ChatClient.ChatClientRequestSpec spec;

    @BeforeEach
    void setUp() {
        AiPersonaService personaService = mock(AiPersonaService.class);
        when(personaService.getEffectiveSystemPrompt()).thenReturn("base");
        properties = new AiChatProperties();
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.build()).thenReturn(chatClient);
        service = new AiChatService(builder, personaService, properties);
        spec = mock(ChatClient.ChatClientRequestSpec.class);
        when(spec.options(any())).thenReturn(spec);
    }

    /** 表驱动：白名单内容 × 请求 model → 是否应调用 options（即是否真正覆盖） */
    @Test
    void tableDrivenWhitelistBehaviour() {
        Object[][] cases = {
                // allowedModels, requestedModel, expectOverride, 说明
                {List.of(), "", false, "空列表+空 model：无覆盖请求"},
                {List.of(), "glm-4-plus", false, "空列表=禁用覆盖"},
                {List.of("glm-4-plus"), "glm-4-plus", true, "命中白名单"},
                {List.of("glm-4-plus"), "gpt-4o", false, "未命中白名单"},
                {List.of("glm-4-plus"), "  glm-4-plus  ", true, "命中白名单（前后空白应被 trim）"},
                {null, "glm-4-plus", false, "白名单未配置（null）等同于空列表"},
                {List.of("a", "b", "c"), "b", true, "多元素白名单命中中间项"},
        };
        for (Object[] c : cases) {
            @SuppressWarnings("unchecked")
            List<String> allowed = (List<String>) c[0];
            String requested = (String) c[1];
            boolean expectOverride = (boolean) c[2];
            String desc = (String) c[3];

            AiChatProperties p = new AiChatProperties();
            p.setAllowedModels(allowed);
            AiChatService svc = newServiceWith(p);
            ChatClient.ChatClientRequestSpec s = mock(ChatClient.ChatClientRequestSpec.class);
            when(s.options(any())).thenReturn(s);

            svc.applyModelOverride(s, requested);

            verify(s, expectOverride ? times(1) : never()).options(any());
            assertNotNull(desc);
        }
    }

    @Test
    void blankModelNeverTriggersOverride() {
        properties.setAllowedModels(List.of("glm-4-plus"));
        service.applyModelOverride(spec, null);
        service.applyModelOverride(spec, "");
        service.applyModelOverride(spec, "   ");
        verify(spec, never()).options(any());
    }

    @Test
    void overrideUsesTrimmedModelName() {
        properties.setAllowedModels(List.of("glm-4-plus"));
        service.applyModelOverride(spec, "  glm-4-plus  ");
        ArgumentCaptor<org.springframework.ai.openai.OpenAiChatOptions> captor =
                ArgumentCaptor.forClass(org.springframework.ai.openai.OpenAiChatOptions.class);
        verify(spec).options(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("glm-4-plus", captor.getValue().getModel(),
                "应携带 trim 后的模型名");
    }

    private AiChatService newServiceWith(AiChatProperties p) {
        AiPersonaService persona = mock(AiPersonaService.class);
        when(persona.getEffectiveSystemPrompt()).thenReturn("base");
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient client = mock(ChatClient.class);
        when(builder.build()).thenReturn(client);
        return new AiChatService(builder, persona, p);
    }

    /** 供将来需要的反射注入（当前未用，保留避免 IDE 提示未使用） */
    @SuppressWarnings("unused")
    private void inject(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }
}
