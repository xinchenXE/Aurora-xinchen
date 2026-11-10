package com.aurora.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置。
 * <p>
 * 队列项 009（ISS-011）：反序列化白名单由 {@code LaissezFaireSubTypeValidator}（不做任何校验）
 * 收敛为 {@code BasicPolymorphicTypeValidator}。原实现对缓存值里的 {@code @class} 不做限制，
 * 一旦 Redis 被写入恶意 payload，反序列化即可加载任意类，属纵深防御缺口。
 * <p>
 * 白名单口径（队列项 009 拍板）：
 * <ul>
 *   <li>{@code com.aurora.} 前缀 —— 本项目全部可缓存类型
 *       （实测仅 {@code com.aurora.remote.RemoteArticleSource} 会带上 {@code @class}）；</li>
 *   <li>必要的 JDK 集合与时间类型 —— 多态集合元素与日期字段所需。</li>
 * </ul>
 * 现网缓存值里带 {@code @class} 的只有 {@code RemoteArticleSource}（命中白名单），
 * 其余键（{@code ai_persona} / {@code website_config} / {@code about} / {@code user_area} /
 * {@code code:*} 等）存的都是 JSON 字符串，本身不带类型信息，故本次收敛对存量键无破坏。
 */
@Configuration
public class RedisConfig {

    /**
     * 反序列化类型白名单。新增可缓存的自定义类型时，只要在 {@code com.aurora.} 包下即自动放行；
     * 包外的类型需显式添加，且必须经过评审。
     */
    private static PolymorphicTypeValidator polymorphicTypeValidator() {
        return BasicPolymorphicTypeValidator.builder()
                // 本项目自有类型：缓存里的 @class 只应来自这里
                .allowIfSubType("com.aurora.")
                // JDK 集合：多态集合元素反序列化需要
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

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(polymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        jackson2JsonRedisSerializer.setObjectMapper(mapper);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
