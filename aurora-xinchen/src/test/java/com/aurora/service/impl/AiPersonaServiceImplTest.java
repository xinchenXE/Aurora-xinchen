package com.aurora.service.impl;

import com.alibaba.fastjson2.JSON;
import com.aurora.config.properties.AiChatProperties;
import com.aurora.entity.AiPersona;
import com.aurora.model.dto.AiPersonaDTO;
import com.aurora.model.vo.AiPersonaVO;
import com.aurora.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static com.aurora.constant.RedisConstant.AI_PERSONA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

/**
 * AiPersonaServiceImpl 单元测试（不依赖 Nacos/Redis/MySQL 上下文）。
 * 覆盖：DB 优先 / Nacos 兜底的三级回退、缓存命中/未命中、
 * 以及旧格式缓存（纯 prompt 字符串）的自愈回归（2026-08-15 修复）。
 */
class AiPersonaServiceImplTest {

    private static final String NACOS_ROLE_NAME = "爱莉希雅";
    private static final String NACOS_GREETING = "嗨~ 想我了嘛🎶，爱莉希雅一直在这里哦~~~";
    private static final String NACOS_SYSTEM_PROMPT = "nacos-default-prompt";

    private RedisService redisService;
    private AiChatProperties properties;
    private AiPersonaServiceImpl service;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        properties = new AiChatProperties();
        properties.setSystemPrompt(NACOS_SYSTEM_PROMPT);
        service = spy(new AiPersonaServiceImpl());
        ReflectionTestUtils.setField(service, "redisService", redisService);
        ReflectionTestUtils.setField(service, "aiChatProperties", properties);
    }

    private AiPersona persona(String roleName, String greeting, String systemPrompt) {
        return AiPersona.builder()
                .id(1)
                .roleName(roleName)
                .greeting(greeting)
                .systemPrompt(systemPrompt)
                .build();
    }

    @Test
    void dbValuesTakePrecedenceOverNacosDefaults() {
        when(redisService.get(AI_PERSONA)).thenReturn(null);
        doReturn(persona("纳西妲", "问题之花", "须弥人设")).when(service).getById(any());

        assertEquals("纳西妲", service.getEffectiveRoleName());
        assertEquals("问题之花", service.getEffectiveGreeting());
        assertEquals("须弥人设", service.getEffectiveSystemPrompt());
        // 回源后缓存应以新 JSON 格式写回（mock 无状态，每次未命中都会重写，故只断言至少一次）
        verify(redisService, atLeastOnce()).set(eq(AI_PERSONA), any());
    }

    @Test
    void blankDbValuesFallBackToNacosDefaults() {
        when(redisService.get(AI_PERSONA)).thenReturn(null);
        doReturn(persona("", "", "")).when(service).getById(any());

        assertEquals(NACOS_ROLE_NAME, service.getEffectiveRoleName());
        assertEquals(NACOS_GREETING, service.getEffectiveGreeting());
        assertEquals(NACOS_SYSTEM_PROMPT, service.getEffectiveSystemPrompt());
    }

    @Test
    void missingDbRowFallsBackToNacosDefaults() {
        when(redisService.get(AI_PERSONA)).thenReturn(null);
        doReturn(null).when(service).getById(any());

        assertEquals(NACOS_ROLE_NAME, service.getEffectiveRoleName());
        assertEquals(NACOS_GREETING, service.getEffectiveGreeting());
        assertEquals(NACOS_SYSTEM_PROMPT, service.getEffectiveSystemPrompt());
    }

    @Test
    void cacheHitWithNewJsonFormatSkipsDb() {
        when(redisService.get(AI_PERSONA))
                .thenReturn(JSON.toJSONString(persona("缓存角色", "缓存开场白", "缓存人设")));

        assertEquals("缓存角色", service.getEffectiveRoleName());
        assertEquals("缓存开场白", service.getEffectiveGreeting());
        assertEquals("缓存人设", service.getEffectiveSystemPrompt());
        verify(service, never()).getById(any());
    }

    @Test
    void legacyPlainTextCacheSelfHealsInsteadOfThrowing() {
        // 2026-08-15 之前的缓存格式：纯 prompt 文本，fastjson 无法解析
        when(redisService.get(AI_PERSONA)).thenReturn("你是 Aurora 博客的 AI 助手，回答简洁、准确、友好。");
        doReturn(persona("DB角色", "DB开场白", "DB人设")).when(service).getById(any());

        assertEquals("DB角色", service.getEffectiveRoleName());
        verify(redisService).del(AI_PERSONA);
        verify(redisService).set(eq(AI_PERSONA), any());
    }

    @Test
    void getAiPersonaReturnsStoredValuesPlusNacosDefaults() {
        when(redisService.get(AI_PERSONA)).thenReturn(null);
        doReturn(persona("纳西妲", "问题之花", "须弥人设")).when(service).getById(any());

        AiPersonaDTO dto = service.getAiPersona();
        assertEquals("纳西妲", dto.getRoleName());
        assertEquals("问题之花", dto.getGreeting());
        assertEquals("须弥人设", dto.getSystemPrompt());
        assertEquals(NACOS_ROLE_NAME, dto.getDefaultRoleName());
        assertEquals(NACOS_GREETING, dto.getDefaultGreeting());
    }

    @Test
    void updateTrimsValuesAndClearsCache() {
        doReturn(true).when(service).saveOrUpdate(any(AiPersona.class));
        AiPersonaVO vo = new AiPersonaVO();
        vo.setRoleName("  角色  ");
        vo.setGreeting("  开场白  ");
        vo.setSystemPrompt(null);

        service.updateAiPersona(vo);

        ArgumentCaptor<AiPersona> captor = ArgumentCaptor.forClass(AiPersona.class);
        verify(service).saveOrUpdate(captor.capture());
        AiPersona saved = captor.getValue();
        assertEquals("角色", saved.getRoleName());
        assertEquals("开场白", saved.getGreeting());
        assertEquals("", saved.getSystemPrompt());
        verify(redisService).del(AI_PERSONA);
    }
}
