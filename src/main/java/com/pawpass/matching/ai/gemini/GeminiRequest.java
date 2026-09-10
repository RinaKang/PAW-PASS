package com.pawpass.matching.ai.gemini;

import java.util.Map;

/**
 * Gemini Interactions API(POST /v1beta/interactions) 요청 바디.
 * 필드명은 프로젝트 전역 Jackson 설정(SNAKE_CASE)을 그대로 따른다 - 마침 Gemini 원문 필드명도
 * snake_case(system_instruction, response_format, mime_type 등)라 별도 @JsonNaming이 필요 없다.
 */
public record GeminiRequest(
        String model,
        String input,
        String systemInstruction,
        ResponseFormat responseFormat
) {
    public record ResponseFormat(String type, String mimeType, Map<String, Object> schema) {
    }
}
