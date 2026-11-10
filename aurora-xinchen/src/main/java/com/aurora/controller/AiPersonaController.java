package com.aurora.controller;

import com.aurora.annotation.OptLog;
import com.aurora.model.dto.AiPersonaDTO;
import com.aurora.model.vo.AiPersonaVO;
import com.aurora.model.vo.ResultVO;
import com.aurora.service.AiPersonaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import static com.aurora.constant.OptTypeConstant.UPDATE;

/**
 * AI 人设设置
 * 模型的 base-url / api-key / model 仍由 Nacos spring.ai.openai.* 配置，此处仅维护人设提示词
 */
@Tag(name = "AI人设设置模块")
@RestController
public class AiPersonaController {

    @Autowired
    private AiPersonaService aiPersonaService;

    @Operation(summary = "查看AI人设")
    @GetMapping("/admin/ai/persona")
    public ResultVO<AiPersonaDTO> getAiPersona() {
        return ResultVO.ok(aiPersonaService.getAiPersona());
    }

    @OptLog(optType = UPDATE)
    @Operation(summary = "修改AI人设")
    @PutMapping("/admin/ai/persona")
    public ResultVO<?> updateAiPersona(@Valid @RequestBody AiPersonaVO aiPersonaVO) {
        aiPersonaService.updateAiPersona(aiPersonaVO);
        return ResultVO.ok();
    }

}
