package com.aurora.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 人设（单行配置，id=1）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName(value = "t_ai_persona")
public class AiPersona {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 角色名称（空串表示回退 Nacos 默认角色名）
     */
    private String roleName;

    /**
     * 开场白（空串表示回退 Nacos 默认开场白）
     */
    private String greeting;

    /**
     * 人设提示词（空串表示回退 Nacos 默认人设）
     */
    private String systemPrompt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
