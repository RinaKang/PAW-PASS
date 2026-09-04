package com.pawpass.matching.ai;

/**
 * 관광지 반려동물 조건 원문(자유서술 텍스트)을 구조화된 판정 결과로 변환하는 인터페이스.
 *
 * ⚠️ 미확정 사항 (팀 논의 필요):
 * - 어떤 LLM/API를 쓸지 (Claude API, OpenAI 등)
 * - 프롬프트 설계
 * - 매 요청마다 실시간 호출할지, 어떤 캐싱/재시도 전략을 쓸지
 * - 파싱 실패 시 폴백 정책 (원문 그대로 노출 등)
 *
 * 지금은 인터페이스만 정의해두고, 확정되면 구현체만 추가하면 되도록
 * MatchingService는 이 인터페이스에만 의존하게 설계할 것.
 */
public interface PetConditionAiParser {

    /**
     * @param rawText 관광지/시설의 반려동물 조건 원문 (여러 필드를 합친 텍스트)
     * @return 파싱된 조건 (허용 여부, 필요 준비물 등)
     */
    ParsedCondition parse(String rawText);

    record ParsedCondition(
            boolean explicitlyAllowed,
            boolean explicitlyDenied,
            String requiredItems,   // 예: "목줄, 이동장"
            String restrictions,    // 예: "맹견 입마개 필수"
            double confidence       // 파싱 신뢰도 (0.0 ~ 1.0)
    ) {
    }
}
