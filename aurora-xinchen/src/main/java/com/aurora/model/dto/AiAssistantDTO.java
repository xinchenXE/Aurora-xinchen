package com.aurora.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 助手展示信息（博客前台聊天窗用，均为生效值）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAssistantDTO {

    /**
     * 角色名称（DB 优先，Nacos 默认兜底）
     */
    private String roleName;

    /**
     * 开场白（DB 优先，Nacos 默认兜底）
     */
    private String greeting;

}
