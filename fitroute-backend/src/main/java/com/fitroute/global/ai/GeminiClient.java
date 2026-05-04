package com.fitroute.global.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

        private final RestClient restClient;
        private final String apiKey;
        private final String model;

        public GeminiClient(
                        @Value("${ai.gemini.api.key}") String apiKey,
                        @Value("${ai.gemini.api.model:gemini-2.5-flash-lite}") String model,
                        @Value("${ai.gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}") String apiUrl) {
                this.apiKey = apiKey;
                this.model = model;
                this.restClient = RestClient.builder()
                                .baseUrl(apiUrl)
                                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .build();
        }

        /**
         * @param systemInstruction AI의 역할 및 응답 형식을 규정 (토큰 절약의 핵심)
         * @param userPrompt        실제 사용자 데이터 및 요청 내용
         */
        public String call(String systemInstruction, String userPrompt) {
                // v1 API에서는 모델명 앞에 models/ 가 붙어야 안정적입니다.
                String endpoint = "/models/" + model + ":generateContent?key=" + apiKey;

                // JSON 스키마를 강제하고 불필요한 대화를 차단하는 구조
                Map<String, Object> requestBody = Map.of(
                                "system_instruction", Map.of(
                                                "parts", List.of(Map.of("text", systemInstruction))),
                                "contents", List.of(
                                                Map.of("role", "user",
                                                                "parts", List.of(Map.of("text", userPrompt)))),
                                "generationConfig", Map.of(
                                                "temperature", 0.2, // 창의성 낮추고 정확도 향상
                                                "response_mime_type", "application/json" // 마크다운 없이 순수 JSON만 반환 [핵심]
                                ));

                log.info("[GeminiClient] Requesting plan generation with model={}", model);

                try {
                        return restClient.post()
                                        .uri(endpoint)
                                        .body(requestBody)
                                        .retrieve()
                                        .body(String.class);
                } catch (HttpClientErrorException e) {
                        log.error("[GeminiClient] API error - status={}, body={}",
                                        e.getStatusCode(), e.getResponseBodyAsString());
                        throw e;
                }
        }
}