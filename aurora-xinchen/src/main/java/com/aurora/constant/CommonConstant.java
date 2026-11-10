package com.aurora.constant;

public interface CommonConstant {

    int ONE = 1;

    int ZERO = 0;

    int FALSE = 0;

    int TRUE = 1;

    int BLOGGER_ID = 1;

    int DEFAULT_CONFIG_ID = 1;

    int DEFAULT_ABOUT_ID = 1;

    String PRE_TAG = "<mark>";

    String POST_TAG = "</mark>";

    String CURRENT = "current";

    String SIZE = "size";

    String DEFAULT_SIZE = "10";

    String DEFAULT_NICKNAME = "用户";

    String COMPONENT = "Layout";

    String UNKNOWN = "未知";

    String APPLICATION_JSON = "application/json;charset=utf-8";

    String CAPTCHA = "验证码";

    String CHECK_REMIND = "审核提醒";

    String COMMENT_REMIND = "评论提醒";

    String MENTION_REMIND = "@提醒";

    /**
     * 操作日志与异常日志的保留天数（队列项 006 拍板口径）。
     * 超过该天数的行由定时任务 auroraQuartz.clearExpiredLogs 清理。
     */
    Integer LOG_RETENTION_DAYS = 90;

    /**
     * 请求级 rolePrompt 的最大长度（队列项 001 拍板口径）。
     * 超过则**拒绝**并返回业务错误码，不截断——便于发现滥用。
     */
    int ROLE_PROMPT_MAX_LENGTH = 2000;

}
