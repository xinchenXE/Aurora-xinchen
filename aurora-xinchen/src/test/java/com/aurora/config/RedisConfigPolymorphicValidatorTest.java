package com.aurora.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 队列项 009：Redis 反序列化白名单的行为验证（不依赖 Spring / Redis）。
 * <p>
 * 用与 {@code RedisConfig} 相同口径的 validator 构造 ObjectMapper，验证：
 * ① 本项目类型与 JDK 集合可通过；② 包外类型（潜在 gadget）被拒绝；
 * ③ 存量键的实际载荷形态（JSON 字符串）不受影响。
 */
class RedisConfigPolymorphicValidatorTest {

    /** 与 RedisConfig.polymorphicTypeValidator() 保持同口径（该方法是 private，此处复刻以便单测） */
    private static PolymorphicTypeValidator validator() {
        return BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.aurora.")
                .allowIfSubType("java.util.ArrayList")
                .allowIfSubType("java.util.LinkedList")
                .allowIfSubType("java.util.HashMap")
                .allowIfSubType("java.util.LinkedHashMap")
                .allowIfSubType("java.util.TreeMap")
                .allowIfSubType("java.util.HashSet")
                .allowIfSubType("java.util.LinkedHashSet")
                .allowIfSubType("java.util.TreeSet")
                .allowIfSubType("java.util.Date")
                .allowIfSubType("java.time.")
                .build();
    }

    private static ObjectMapper mapper() {
        ObjectMapper m = new ObjectMapper();
        m.activateDefaultTyping(validator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return m;
    }

    /** 缓存里实际存在的形态：RemoteArticleSource 带 @class */
    private static final String REMOTE_SOURCE_JSON =
            "{\"@class\":\"com.aurora.remote.RemoteArticleSource\","
                    + "\"source\":\"orangejun.cn\",\"sourceName\":\"橘子君\","
                    + "\"apiUrl\":\"https://orangejun.cn/api/articles/all\","
                    + "\"handlerName\":\"com.aurora.remote.adapter.DefaultRemoteArticleAdapter\"}";

    @Test
    void projectOwnTypesAreAccepted() throws Exception {
        Object restored = mapper().readValue(REMOTE_SOURCE_JSON, Object.class);
        assertEquals("com.aurora.remote.RemoteArticleSource", restored.getClass().getName());
    }

    @Test
    void jdkCollectionsAreAccepted() throws Exception {
        String json = mapper().writeValueAsString(new ArrayList<>(List.of("a", "b")));
        Object restored = mapper().readValue(json, Object.class);
        assertTrue(restored instanceof ArrayList, "ArrayList 应被白名单接受: " + restored.getClass());

        String mapJson = mapper().writeValueAsString(new HashMap<>(Map.of("k", "v")));
        assertDoesNotThrow(() -> mapper().readValue(mapJson, Object.class));
    }

    @Test
    void outOfPackageTypesAreRejected() {
        // 潜在 gadget：包外类型必须被拒。用 JDK 里一个"会被默认类型化"的包外类做样本。
        String malicious = "{\"@class\":\"javax.script.ScriptEngineManager\"}";
        Exception e = assertThrows(Exception.class,
                () -> mapper().readValue(malicious, Object.class),
                "包外类型不应被反序列化");
        assertTrue(e.getMessage().toLowerCase().contains("not allowed")
                        || e.getMessage().toLowerCase().contains("denied")
                        || e.getMessage().toLowerCase().contains("invalid"),
                "应为白名单拒绝信息，实际: " + e.getMessage());
    }

    @Test
    void plainJsonStringPayloadsAreUnaffected() throws Exception {
        // 存量键（ai_persona / website_config / about / user_area）存的是 JSON 字符串本身，
        // 不带 @class，反序列化目标就是 String —— 白名单不参与，故不受影响。
        String stored = "\"{\\\"greeting\\\":\\\"hi\\\"}\"";
        Object restored = mapper().readValue(stored, Object.class);
        assertEquals("{\"greeting\":\"hi\"}", restored);
    }
}
