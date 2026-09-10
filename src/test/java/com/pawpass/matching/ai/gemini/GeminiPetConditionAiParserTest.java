package com.pawpass.matching.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.pawpass.matching.ai.PetConditionAiParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ObjectMapper는 실제 앱과 동일하게 SNAKE_CASE 전략으로 직접 구성한다 (@SpringBootTest 없이도
 * application.yml의 spring.jackson.property-naming-strategy: SNAKE_CASE와 같은 조건을 재현하기 위함).
 */
@ExtendWith(MockitoExtension.class)
class GeminiPetConditionAiParserTest {

    @Mock
    private GeminiApiClient geminiApiClient;

    @Test
    void snake_case_응답을_ParsedCondition으로_올바르게_매핑한다() {
        ObjectMapper objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        GeminiPetConditionAiParser parser = new GeminiPetConditionAiParser(geminiApiClient, objectMapper);

        String json = """
                {
                  "explicitly_allowed": true,
                  "explicitly_denied": false,
                  "required_items": "목줄, 이동장",
                  "restrictions": "소형견만 가능",
                  "confidence": 0.9
                }
                """;
        when(geminiApiClient.generateJson(anyString(), anyString(), any())).thenReturn(json);

        PetConditionAiParser.ParsedCondition result = parser.parse("소형견에 한해 목줄과 이동장을 지참하면 동반 가능합니다.");

        assertThat(result.explicitlyAllowed()).isTrue();
        assertThat(result.explicitlyDenied()).isFalse();
        assertThat(result.requiredItems()).isEqualTo("목줄, 이동장");
        assertThat(result.restrictions()).isEqualTo("소형견만 가능");
        assertThat(result.confidence()).isEqualTo(0.9);
    }

    @Test
    void 스키마를_벗어난_응답이면_예외를_던진다() {
        ObjectMapper objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        GeminiPetConditionAiParser parser = new GeminiPetConditionAiParser(geminiApiClient, objectMapper);

        when(geminiApiClient.generateJson(anyString(), anyString(), any())).thenReturn("이건 JSON이 아님");

        assertThatThrownBy(() -> parser.parse("아무 원문"))
                .isInstanceOf(IllegalStateException.class);
    }
}
