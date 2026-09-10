package com.pawpass.matching.ai;

/**
 * 관광지 반려동물 조건 원문(자유서술 텍스트)을 구조화된 판정 결과로 변환하는 인터페이스.
 * 구현체: {@link com.pawpass.matching.ai.gemini.GeminiPetConditionAiParser} (Gemini 무료 티어 Flash 모델),
 * {@link CachingPetConditionAiParser}(@Primary - rawText를 키로 판정 결과만 캐싱, 원문 자체를 실시간으로
 * 다시 가져오는 것과는 무관 - TourAPI 호출은 MatchingService가 매번 그대로 수행함).
 *
 * ⚠️ 아직 남은 사항:
 * - 파싱/호출 실패 시 폴백 정책 (원문 그대로 노출 등) - 지금 구현체는 예외를 그대로 던짐
 *
 * MatchingService는 이 인터페이스에만 의존하도록 설계할 것.
 */
public interface PetConditionAiParser {

    /**
     * @param rawText 관광지/시설의 반려동물 조건 원문 (여러 필드를 합친 텍스트)
     * @return 파싱된 조건 (허용 여부, 크기/체중 제한, 필요 준비물 등)
     */
    ParsedCondition parse(String rawText);

    /**
     * maxWeightKg/maxSizeCategory는 원문에 숫자/크기 분류가 "명시적으로" 있을 때만 채워진다 (없으면 null).
     * 둘 다 없으면 크기 기준 자체가 원문에 없다는 뜻이지, 아무나 가능하다는 뜻은 아니다 - 최종 허용 여부는
     * explicitlyAllowed/explicitlyDenied와 별개로 판단한다. maxSizeCategory는 "이 크기까지만 허용"을 뜻하며
     * {@link com.pawpass.pet.domain.PetSize}의 SMALL/MEDIUM/LARGE 문자열 중 하나다
     * (소형 10kg 미만 / 중형 10kg 이상~25kg 미만 / 대형 25kg 이상 - 서비스 전체 분류 기준과 동일).
     */
    record ParsedCondition(
            boolean explicitlyAllowed,
            boolean explicitlyDenied,
            Double maxWeightKg,      // 예: "5kg 이하만 가능" -> 5.0, 원문에 숫자 기준이 없으면 null
            String maxSizeCategory,  // 예: "소형견만 가능" -> "SMALL", 원문에 크기 분류 언급이 없으면 null
            String requiredItems,    // 예: "목줄, 이동장"
            String restrictions,     // 예: "맹견 입마개 필수"
            double confidence        // 파싱 신뢰도 (0.0 ~ 1.0)
    ) {
    }
}
