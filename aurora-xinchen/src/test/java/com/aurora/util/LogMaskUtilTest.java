package com.aurora.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LogMaskUtil 单元测试（不依赖 Spring 上下文）。
 * 覆盖：队列项 006 拍板的黑名单（password/oldPassword/newPassword/code/token）、
 * 大小写不敏感、递归 JSON 树、数组元素、MultipartFile 分支之外的入参形态。
 */
class LogMaskUtilTest {

    @Test
    void masksTopLevelPasswordKeys() {
        String json = LogMaskUtil.toMaskedJson(Map.of(
                "oldPassword", "OldPlainText",
                "newPassword", "NewPlainText"
        ));
        assertFalse(json.contains("OldPlainText"), "oldPassword 原文不应出现: " + json);
        assertFalse(json.contains("NewPlainText"), "newPassword 原文不应出现: " + json);
        assertTrue(json.contains("\"oldPassword\":\"***\""), json);
        assertTrue(json.contains("\"newPassword\":\"***\""), json);
    }

    @Test
    void keyMatchingIsCaseInsensitive() {
        // 黑名单以全小写比较：Password / PASSWORD / pAsSwOrD 都应命中
        for (String key : List.of("password", "Password", "PASSWORD", "pAsSwOrD")) {
            String json = LogMaskUtil.toMaskedJson(Map.of(key, "Secret123"));
            assertFalse(json.contains("Secret123"), "键 " + key + " 未脱敏: " + json);
            assertTrue(json.contains("\"***\""), json);
        }
    }

    @Test
    void masksCodeAndToken() {
        String json = LogMaskUtil.toMaskedJson(Map.of(
                "code", "123456",
                "token", "eyJhbGciOi.payload.sig"
        ));
        assertFalse(json.contains("123456"), json);
        assertFalse(json.contains("eyJhbGciOi"), json);
    }

    @Test
    void doesNotMaskUnrelatedKeys() {
        String json = LogMaskUtil.toMaskedJson(Map.of(
                "username", "admin@163.com",
                "nickname", "演示账号"
        ));
        assertTrue(json.contains("admin@163.com"), "非敏感字段应保持原值: " + json);
        assertTrue(json.contains("演示账号"), json);
    }

    @Test
    void recursesIntoNestedObjects() {
        Map<String, Object> nested = Map.of(
                "user", Map.of("name", "bob", "password", "DeepPlain"),
                "note", "ok"
        );
        String json = LogMaskUtil.toMaskedJson(nested);
        assertFalse(json.contains("DeepPlain"), "嵌套对象内的密码应被脱敏: " + json);
        assertTrue(json.contains("bob"), json);
    }

    @Test
    void recursesIntoArrayElements() {
        List<Object> args = List.of(
                Map.of("username", "alice", "password", "ArrayPlain"),
                Map.of("code", "888888")
        );
        String json = LogMaskUtil.toMaskedJson(args);
        assertFalse(json.contains("ArrayPlain"), "数组元素内的密码应被脱敏: " + json);
        assertFalse(json.contains("888888"), json);
        assertTrue(json.contains("alice"), json);
    }

    @Test
    void deepNestingObjectInsideArrayInsideObject() {
        JSONObject deep = new JSONObject();
        deep.put("newPassword", "VeryDeepPlain");
        JSONArray arr = new JSONArray();
        arr.add(deep);
        JSONObject root = new JSONObject();
        root.put("payload", arr);
        root.put("token", "Tok");

        String json = LogMaskUtil.toMaskedJson(root);
        assertFalse(json.contains("VeryDeepPlain"), json);
        assertFalse(json.contains("\"Tok\""), json);
    }

    @Test
    void nullAndPrimitivesAreHandled() {
        assertNull(LogMaskUtil.toMaskedJson(null));
        assertEquals("\"hello\"", LogMaskUtil.toMaskedJson("hello"));
        assertEquals("42", LogMaskUtil.toMaskedJson(42));
    }

    @Test
    void maskOfParsedJsonKeepsStructure() {
        JSONObject parsed = JSON.parseObject("{\"a\":1,\"password\":\"x\",\"b\":{\"password\":\"y\"}}");
        String json = JSON.toJSONString(LogMaskUtil.mask(parsed));
        assertTrue(json.contains("\"a\":1"), json);
        assertFalse(json.contains("\"x\""), json);
        assertFalse(json.contains("\"y\""), json);
    }
}
