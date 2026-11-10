package com.aurora.controller;

import com.aurora.ai.AiChatService;
import com.aurora.constant.CommonConstant;
import com.aurora.enums.RoleEnum;
import com.aurora.enums.StatusCodeEnum;
import com.aurora.model.dto.AiAssistantDTO;
import com.aurora.model.vo.ResultVO;
import com.aurora.service.AiPersonaService;
import com.aurora.util.UserUtil;
import lombok.extern.log4j.Log4j2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI 对话接口（Spring AI）
 * <p>
 * 队列项 001（ISS-001 拍板 B+A 组合）收口口径：
 * <ul>
 *   <li>{@code model} 仅 admin 可用（越权/非白名单一律回落默认模型）；</li>
 *   <li>{@code rolePrompt} 仅 admin 可用，且限长 {@code CommonConstant.ROLE_PROMPT_MAX_LENGTH}，
 *       超长**拒绝**（返回 {@code 52004}）而非截断。</li>
 * </ul>
 * 依据：前端全仓无发送点，收口不影响任何现有 UI；admin 逃生口保留 owner 用 API 调人设/换模型的能力。
 */
@Log4j2
@Tag(name = "AI对话模块")
@RestController
@RequestMapping("/ai")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private AiPersonaService aiPersonaService;

    @Operation(summary = "AI 助手展示信息（角色名 + 开场白，均为生效值）")
    @GetMapping("/assistant")
    public ResultVO<AiAssistantDTO> getAssistant() {
        return ResultVO.ok(AiAssistantDTO.builder()
                .roleName(aiPersonaService.getEffectiveRoleName())
                .greeting(aiPersonaService.getEffectiveGreeting())
                .build());
    }

    @Operation(summary = "AI 对话（同步）")
    @PostMapping("/chat")
    public ResultVO<String> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        if (message.isBlank()) {
            return ResultVO.fail("消息不能为空");
        }
        // rolePrompt 可选：角色扮演提示词，传入后模型按该人设回答；model 可选：本次请求临时切换模型
        String rolePrompt = body.getOrDefault("rolePrompt", "");
        String model = body.getOrDefault("model", "");
        if (!isAdmin()) {
            // 非 admin：两个覆盖参数一律忽略，回落默认人设与默认模型（ISS-001 收口）
            if (isNotBlank(model) || isNotBlank(rolePrompt)) {
                log.warn("非 admin 用户尝试使用 AI 覆盖参数，已忽略：uri=/ai/chat");
            }
            rolePrompt = "";
            model = "";
        }
        if (isNotBlank(rolePrompt) && rolePrompt.trim().length() > CommonConstant.ROLE_PROMPT_MAX_LENGTH) {
            return ResultVO.fail(StatusCodeEnum.ROLE_PROMPT_TOO_LONG.getCode(),
                    StatusCodeEnum.ROLE_PROMPT_TOO_LONG.getDesc()
                            + "（上限 " + CommonConstant.ROLE_PROMPT_MAX_LENGTH + " 字）");
        }
        return ResultVO.ok(aiChatService.chat(message, rolePrompt, model));
    }

    @Operation(summary = "AI 对话（流式 SSE）")
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam("message") String message,
                                   @RequestParam(required = false) String rolePrompt,
                                   @RequestParam(required = false) String model) {
        // rolePrompt 可选：角色扮演提示词；model 可选：本次请求临时切换模型
        String rp = rolePrompt == null ? "" : rolePrompt;
        String md = model == null ? "" : model;
        if (!isAdmin()) {
            if (isNotBlank(md) || isNotBlank(rp)) {
                log.warn("非 admin 用户尝试使用 AI 覆盖参数，已忽略：uri=/ai/chat/stream");
            }
            rp = "";
            md = "";
        }
        if (isNotBlank(rp) && rp.trim().length() > CommonConstant.ROLE_PROMPT_MAX_LENGTH) {
            return Flux.just(StatusCodeEnum.ROLE_PROMPT_TOO_LONG.getDesc()
                    + "（上限 " + CommonConstant.ROLE_PROMPT_MAX_LENGTH + " 字）");
        }
        return aiChatService.chatStream(message, rp, md);
    }

    /**
     * 当前登录用户是否为 admin。未登录/无认证上下文一律视为非 admin
     * （匿名面已由资源登记挡在 40001，这里只做纵深防御）。
     * <p>包级可见以便单测直接覆盖角色判定分支（ISS-001 收口的核心条件）。
     */
    boolean isAdmin() {
        try {
            Authentication authentication = UserUtil.getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> RoleEnum.ADMIN.getLabel().equals(a.getAuthority()));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
