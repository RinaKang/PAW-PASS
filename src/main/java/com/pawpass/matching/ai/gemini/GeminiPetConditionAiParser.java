package com.pawpass.matching.ai.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawpass.matching.ai.PetConditionAiParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Gemini(무료 티어 Flash 모델)로 반려동물 동반 조건 원문을 구조화된 판정 결과로 변환
 * response_format.schema로 구조화 출력을 강제하므로 파싱 실패 가능성은 낮지만,
 * 만에 하나 스키마를 벗어난 응답이 오면 예외를 던진다 - 호출부(MatchingService)에서 폴백 정책을 정할 것.
 */
@Component
@RequiredArgsConstructor
public class GeminiPetConditionAiParser implements PetConditionAiParser {

    private static final String SYSTEM_INSTRUCTION = """
            너는 관광지/문화시설의 반려동물 동반 조건 원문을 읽고 판정 결과를 JSON으로만 답하는 도우미다.
            - explicitly_allowed: 원문에 반려동물 동반이 명시적으로 가능하다고 나와 있으면 true, 아니면 false.
            - explicitly_denied: 원문에 반려동물 동반이 명시적으로 불가/금지라고 나와 있으면 true, 아니면 false.
              (원문이 애매하거나 조건부라면 둘 다 false로 둔다.)
            - max_weight_kg: 원문에 체중 상한이 숫자로 명시된 경우만 그 값(kg, 숫자)을 넣는다.
              예: "5kg 이하만 가능" -> 5. 숫자로 된 체중 기준이 원문에 아예 없으면 이 필드를 생략한다(넣지 않는다).
            - max_size_category: 원문에 "소형견만"/"중형견까지" 같은 크기 분류로 된 제한이 있을 때만
              그 중 가장 큰 허용 크기를 SMALL, MEDIUM, LARGE 중 하나의 문자열로 넣는다
              (소형=10kg 미만, 중형=10kg 이상~25kg 미만, 대형=25kg 이상 기준).
              예: "소형견만 가능" -> "SMALL", "중형견까지 가능" -> "MEDIUM". 크기 분류 언급이 원문에 없으면 이 필드를 생략한다.
            - required_items: 동반 시 필요한 준비물(목줄, 이동장, 접종증명서 등)을 요약. 없으면 빈 문자열.
            - restrictions: 크기/견종/구역 제한 등 조건을 요약(사람이 읽을 문장). 없으면 빈 문자열.
            - confidence: 이 판정에 대한 확신도, 0.0~1.0 사이 숫자. 원문이 짧거나 모호할수록 낮게.
            """;

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "explicitly_allowed", Map.of("type", "boolean"),
                    "explicitly_denied", Map.of("type", "boolean"),
                    "max_weight_kg", Map.of("type", "number"),
                    "max_size_category", Map.of("type", "string", "enum", List.of("SMALL", "MEDIUM", "LARGE")),
                    "required_items", Map.of("type", "string"),
                    "restrictions", Map.of("type", "string"),
                    "confidence", Map.of("type", "number")
            ),
            // max_weight_kg/max_size_category는 의도적으로 제외 - 원문에 근거가 없으면 필드 자체를 생략하게 해서
            // Double/String 필드가 자연스럽게 null이 되도록 한다 (0이나 빈 문자열 같은 값으로 "제한 없음"을 흉내내지 않음).
            "required", List.of("explicitly_allowed", "explicitly_denied", "required_items", "restrictions", "confidence")
    );

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    @Override
    public ParsedCondition parse(String rawText) {
        String json = geminiApiClient.generateJson(SYSTEM_INSTRUCTION, rawText, SCHEMA);
        try {
            return objectMapper.readValue(json, ParsedCondition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Gemini 응답을 ParsedCondition으로 파싱하지 못했습니다: " + json, e);
        }
    }
}
