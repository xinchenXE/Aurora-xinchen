package com.aurora.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.Map;
import java.util.Set;

/**
 * 操作日志脱敏工具。
 * <p>
 * 背景（ISS-017）：{@code OperationLogAspect} 原先把全部入参与响应体整棵树序列化进
 * {@code t_operation_log.request_param / response_data}，而"修改密码""修改管理员密码"
 * 两个接口恰好带 {@code @OptLog}，明文密码因此有落库路径。
 * <p>
 * 口径（队列项 006 拍板）：字段名在黑名单内的值一律替换为 {@code ***}；
 * 大小写不敏感；递归整棵 JSON 树（含数组元素与嵌套对象）；入参与响应体同走。
 * 黑名单由拍板钉死，不放宽也不扩张。
 */
public final class LogMaskUtil {

    /**
     * 敏感字段黑名单（小写比较）。清单来自队列项 006 拍板，修改需重新拍板。
     */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "oldpassword", "newpassword", "code", "token"
    );

    private static final String MASK = "***";

    private LogMaskUtil() {
    }

    /**
     * 把任意对象转成 JSON 后再做字段级脱敏，返回脱敏后的 JSON 字符串。
     * 序列化失败时降级为固定文案，绝不因为脱敏把主流程打挂。
     */
    public static String toMaskedJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return JSON.toJSONString(mask(JSON.toJSON(value)));
        } catch (Exception e) {
            return "[unserializable]";
        }
    }

    /**
     * 递归脱敏：JSONObject 按键名判断，JSONArray 逐元素下钻，其余原样返回。
     * 可见于单测，故包级可见而非私有。
     */
    static Object mask(Object node) {
        if (node instanceof JSONObject obj) {
            JSONObject masked = new JSONObject(obj.size());
            for (Map.Entry<String, Object> entry : obj.entrySet()) {
                String key = entry.getKey();
                if (key != null && SENSITIVE_KEYS.contains(key.toLowerCase())) {
                    masked.put(key, MASK);
                } else {
                    masked.put(key, mask(entry.getValue()));
                }
            }
            return masked;
        }
        if (node instanceof JSONArray arr) {
            JSONArray masked = new JSONArray(arr.size());
            for (Object item : arr) {
                masked.add(mask(item));
            }
            return masked;
        }
        return node;
    }
}
