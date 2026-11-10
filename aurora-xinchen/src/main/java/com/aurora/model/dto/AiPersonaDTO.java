package com.aurora.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 人设信息
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPersonaDTO {

    /**
     * 角色名称（空串表示当前使用 Nacos 默认角色名）
     */
    private String roleName;

    /**
     * 开场白（空串表示当前使用 Nacos 默认开场白）
     */
    private String greeting;

    /**
     * 人设提示词（空串表示当前使用 Nacos 默认人设）
     */
    private String systemPrompt;

    /**
     * Nacos 当前默认角色名（管理端占位提示用）
     */
    private String defaultRoleName;

    /**
     * Nacos 当前默认开场白（管理端占位提示用）
     */
    private String defaultGreeting;

}
