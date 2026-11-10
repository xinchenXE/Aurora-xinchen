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
public class JobLogSearchVO {

    @Schema(description = "jobId")
    private Integer jobId;

    @Schema(description = "jobName")
    private String jobName;

    @Schema(description = "jobGroup")
    private String jobGroup;

    @Schema(description = "status")
    private Integer status;

    @Schema(description = "startTime")
    private String startTime;

    @Schema(description = "endTime")
    private String endTime;
}
