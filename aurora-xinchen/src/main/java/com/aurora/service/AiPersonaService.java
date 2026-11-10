package com.aurora.service;

import com.aurora.entity.AiPersona;
import com.aurora.model.dto.AiPersonaDTO;
import com.aurora.model.vo.AiPersonaVO;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * AI 人设服务
 */
public interface AiPersonaService extends IService<AiPersona> {

    /**
     * 查看当前人设（后台展示用，空串表示使用 Nacos 默认人设）
     */
    AiPersonaDTO getAiPersona();

    /**
     * 修改人设（systemPrompt 留空表示恢复 Nacos 默认人设）
     */
    void updateAiPersona(AiPersonaVO aiPersonaVO);

    /**
     * 获取生效中的系统提示词：DB 人设优先，为空时回退 Nacos aurora.ai.chat.system-prompt
     */
    String getEffectiveSystemPrompt();

    /**
     * 获取生效中的角色名称：DB 优先，为空时回退 Nacos aurora.ai.chat.role-name
     */
    String getEffectiveRoleName();

    /**
     * 获取生效中的开场白：DB 优先，为空时回退 Nacos aurora.ai.chat.greeting
     */
    String getEffectiveGreeting();

}
