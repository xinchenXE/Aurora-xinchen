package com.aurora.service.impl;

import com.alibaba.fastjson2.JSON;
import com.aurora.config.properties.AiChatProperties;
import com.aurora.entity.AiPersona;
import com.aurora.mapper.AiPersonaMapper;
import com.aurora.model.dto.AiPersonaDTO;
import com.aurora.model.vo.AiPersonaVO;
import com.aurora.service.AiPersonaService;
import com.aurora.service.RedisService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static com.aurora.constant.CommonConstant.DEFAULT_CONFIG_ID;
import static com.aurora.constant.RedisConstant.AI_PERSONA;

/**
 * AI 人设服务实现（单行配置 + Redis 缓存，复刻网站配置模式）
 */
@Service
public class AiPersonaServiceImpl extends ServiceImpl<AiPersonaMapper, AiPersona> implements AiPersonaService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private AiChatProperties aiChatProperties;

    @Override
    public AiPersonaDTO getAiPersona() {
        AiPersona persona = getCachedPersona();
        return AiPersonaDTO.builder()
                .roleName(persona.getRoleName())
                .greeting(persona.getGreeting())
                .systemPrompt(persona.getSystemPrompt())
                .defaultRoleName(aiChatProperties.getRoleName())
                .defaultGreeting(aiChatProperties.getGreeting())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateAiPersona(AiPersonaVO aiPersonaVO) {
        AiPersona aiPersona = AiPersona.builder()
                .id(DEFAULT_CONFIG_ID)
                .roleName(trimToEmpty(aiPersonaVO.getRoleName()))
                .greeting(trimToEmpty(aiPersonaVO.getGreeting()))
                .systemPrompt(trimToEmpty(aiPersonaVO.getSystemPrompt()))
                .build();
        this.saveOrUpdate(aiPersona);
        redisService.del(AI_PERSONA);
    }

    @Override
    public String getEffectiveSystemPrompt() {
        String prompt = getCachedPersona().getSystemPrompt();
        if (prompt == null || prompt.isBlank()) {
            // DB 未配置人设时回退 Nacos 默认人设（aurora.ai.chat.system-prompt）
            return aiChatProperties.getSystemPrompt();
        }
        return prompt;
    }

    @Override
    public String getEffectiveRoleName() {
        String roleName = getCachedPersona().getRoleName();
        if (roleName == null || roleName.isBlank()) {
            // DB 未配置角色名时回退 Nacos 默认（aurora.ai.chat.role-name）
            return aiChatProperties.getRoleName();
        }
        return roleName;
    }

    @Override
    public String getEffectiveGreeting() {
        String greeting = getCachedPersona().getGreeting();
        if (greeting == null || greeting.isBlank()) {
            // DB 未配置开场白时回退 Nacos 默认（aurora.ai.chat.greeting）
            return aiChatProperties.getGreeting();
        }
        return greeting;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private AiPersona getCachedPersona() {
        Object cache = redisService.get(AI_PERSONA);
        if (Objects.nonNull(cache)) {
            try {
                AiPersona cached = JSON.parseObject(cache.toString(), AiPersona.class);
                if (Objects.nonNull(cached)) {
                    return cached;
                }
            } catch (Exception e) {
                // 兼容旧格式缓存（2026-08-15 前存的是纯 prompt 字符串，fastjson 无法解析）：
                // 删除脏键回源 DB，本方法末尾会以新 JSON 格式重写缓存，实现自愈
                redisService.del(AI_PERSONA);
            }
        }
        AiPersona aiPersona = this.getById(DEFAULT_CONFIG_ID);
        if (Objects.isNull(aiPersona)) {
            aiPersona = AiPersona.builder()
                    .id(DEFAULT_CONFIG_ID)
                    .roleName("")
                    .greeting("")
                    .systemPrompt("")
                    .build();
        }
        redisService.set(AI_PERSONA, JSON.toJSONString(aiPersona));
        return aiPersona;
    }

}
