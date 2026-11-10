package com.aurora.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "网站配置")
public class WebsiteConfigVO {

    @Schema(description = "网站名称")
    private String name;

    @Schema(description = "网站作者昵称")
    private String englishName;

    @Schema(description = "网站作者")
    private String author;

    @Schema(description = "网站头像")
    private String authorAvatar;

    @Schema(description = "网站作者介绍")
    private String authorIntro;

    @Schema(description = "网站logo")
    private String logo;

    @Schema(description = "多语言")
    private Integer multiLanguage;

    @Schema(description = "网站公告")
    private String notice;

    @Schema(description = "网站创建时间")
    private String websiteCreateTime;

    @Schema(description = "网站备案号")
    private String beianNumber;

    @Schema(description = "QQ登录")
    private Integer qqLogin;

    @Schema(description = "github")
    private String github;

    @Schema(description = "gitee")
    private String gitee;

    @Schema(description = "qq")
    private String qq;

    @Schema(description = "微信")
    private String weChat;

    @Schema(description = "微博")
    private String weibo;

    @Schema(description = "csdn")
    private String csdn;

    @Schema(description = "zhihu")
    private String zhihu;

    @Schema(description = "juejin")
    private String juejin;

    @Schema(description = "twitter")
    private String twitter;

    @Schema(description = "stackoverflow")
    private String stackoverflow;

    @Schema(description = "游客头像")
    private String touristAvatar;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "是否评论审核")
    private Integer isCommentReview;

    @Schema(description = "是否邮箱通知")
    private Integer isEmailNotice;

    @Schema(description = "是否打赏")
    private Integer isReward;

    @Schema(description = "微信二维码")
    private String weiXinQRCode;

    @Schema(description = "支付宝二维码")
    private String alipayQRCode;

}
