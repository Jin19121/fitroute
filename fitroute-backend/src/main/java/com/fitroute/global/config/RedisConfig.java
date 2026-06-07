// global/config/RedisConfig.java
package com.fitroute.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    /**
     * DashboardResponse JSON 직렬화/역직렬화용 ObjectMapper
     *
     * JavaTimeModule 등록 이유:
     * DashboardResponse에 LocalDate 타입 필드가 있음.
     * 기본 ObjectMapper는 LocalDate를 배열([2025,6,7])로 직렬화하는데,
     * JavaTimeModule을 등록하면 "2025-06-07" 문자열로 처리되어
     * Redis에서 꺼낼 때 역직렬화가 정상 동작함.
     *
     * WRITE_DATES_AS_TIMESTAMPS 비활성화:
     * 날짜를 숫자 타임스탬프가 아닌 ISO-8601 문자열로 저장하기 위함.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}