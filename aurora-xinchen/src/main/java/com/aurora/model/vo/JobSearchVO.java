package com.aurora.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchVO {

    @Schema(description = "jobName")
    private String jobName;

    @Schema(description = "jobGroup")
    private String jobGroup;

    @Schema(description = "status")
    private Integer status;
}
