package com.aurora.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 人设修改请求
 */
@Data
@Schema(description = "AI人设")
public class AiPersonaVO {

    @Schema(description = "角色名称，留空表示恢复 Nacos 默认角色名")
    @Size(max = 20, message = "角色名称不能超过20字")
    private String roleName;

    @Schema(description = "开场白，留空表示恢复 Nacos 默认开场白")
    @Size(max = 200, message = "开场白不能超过200字")
    private String greeting;

    @Schema(description = "人设提示词，留空表示恢复 Nacos 默认人设")
    @Size(max = 2000, message = "人设提示词不能超过2000字")
    private String systemPrompt;

}
