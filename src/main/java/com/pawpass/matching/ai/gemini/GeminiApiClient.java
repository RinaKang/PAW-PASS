package com.pawpass.matching.ai.gemini;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini API(Interactions API, https://ai.google.dev/api/interactions-api) 호출 클라이언트.
 * 무료 티어 Flash 계열 모델 사용을 전제로 한다 (application.yml의 external-api.gemini-api.model).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    private final WebClient geminiApiWebClient;

    @Value("${external-api.gemini-api.api-key}")
    private String apiKey;

    @Value("${external-api.gemini-api.model}")
    private String model;

    /**
     * @param systemInstruction 모델 역할/출력 형식 지시
     * @param input             실제 판정 대상 원문
     * @param schema            response_format.schema로 넘길 JSON Schema (구조화 출력 강제)
     * @return 스키마에 맞는 JSON 문자열 (steps[].content[].text)
     */
    public String generateJson(String systemInstruction, String input, Map<String, Object> schema) {
        GeminiRequest request = new GeminiRequest(
                model, input, systemInstruction,
                new GeminiRequest.ResponseFormat("text", "application/json", schema)
        );

        GeminiResponse response;
        try {
            response = geminiApiWebClient.post()
                    .uri("/v1beta/interactions")
                    .header("x-goog-api-key", apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Gemini 호출 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }

        if (response == null || response.steps() == null) {
            throw new IllegalStateException("Gemini 응답이 비어 있습니다.");
        }

        return response.steps().stream()
                .filter(step -> "model_output".equals(step.type()))
                .flatMap(step -> step.content() == null ? List.<GeminiResponse.Content>of().stream() : step.content().stream())
                .filter(content -> "text".equals(content.type()))
                .map(GeminiResponse.Content::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Gemini 응답에서 텍스트를 찾지 못했습니다: " + response));
    }
}
