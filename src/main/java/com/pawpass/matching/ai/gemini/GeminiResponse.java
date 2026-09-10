package com.pawpass.matching.ai.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Gemini Interactions API 응답. 생성된 텍스트는 steps[].content[].text 경로에 있다
 * (steps[].type == "model_output", content[].type == "text").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(String status, List<Step> steps) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Step(String type, List<Content> content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(String type, String text) {
    }
}
