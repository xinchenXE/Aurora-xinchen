package com.aurora.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "资源")
public class ResourceVO {

    @Schema(description = "资源id")
    private Integer id;

    @NotBlank(message = "资源名不能为空")
    @Schema(description = "资源名")
    private String resourceName;

    @Schema(description = "资源路径")
    private String url;

    @Schema(description = "资源路径")
    private String requestMethod;

    @Schema(description = "父资源id")
    private Integer parentId;

    @Schema(description = "是否匿名访问")
    private Integer isAnonymous;

}
