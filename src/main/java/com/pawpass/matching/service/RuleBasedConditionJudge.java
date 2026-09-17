package com.pawpass.matching.service;

import com.pawpass.matching.ai.PetConditionAiParser.ParsedCondition;
import com.pawpass.matching.dto.MatchResponse;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 호출 전에 규칙만으로 판정 가능한 경우를 최대한 걸러내서 Gemini 호출을 아낀다 (하이브리드 매칭의
 * 규칙 기반 절반). Gemini 무료 티어가 분당 5회로 제한된다는 걸 실측으로 확인한 뒤(2026-09-10) 비용/
 * 레이트리밋을 줄이려고 두 번째 단계(tryExtractStructured)를 추가함 - "숫자+kg+이하", "소형견만" 같은
 * 정형화된 표현은 굳이 AI를 안 거쳐도 기계적으로 뽑아낼 수 있다.
 *
 * 오탐(특히 거짓 "가능"/"불가" 판정)이 훨씬 치명적이므로, 조금이라도 애매하면 전부 AI로 넘긴다.
 */
final class RuleBasedConditionJudge {

    private static final List<String> DENY_KEYWORDS = List.of(
            "동반 불가", "출입 불가", "반려동물 금지", "동물 금지", "출입금지", "반려동물 동반 불가"
    );
    // "모두 가능"은 KCISA 실데이터(pet_facilities.allowed_pet_size)에서 압도적으로 가장 흔한 값이라
    // 실측 데이터 확인 후 추가함(2026-09-10) - 이거 하나만으로도 facility 쪽 AI 호출이 크게 줄어든다.
    private static final List<String> UNCONDITIONAL_ALLOW_KEYWORDS = List.of(
            "전 견종 가능", "모든 반려동물 동반 가능", "모든 동물 동반 가능", "견종 제한 없이", "모두 가능"
    );
    // 이 중 하나라도 있으면 "무조건 가능"으로 단정하지 않는다 (크기/무게/견종 제한이 숨어있을 수 있음).
    // "제한"은 따로 뺐다 - "제한사항 없음"처럼 부정형으로 훨씬 자주 쓰여서(실데이터 확인) 단순 포함 검사로는
    // 정반대로 해석하게 된다. 아래 hasRealRestrictionMention()에서 부정형을 걸러내고 따로 처리한다.
    private static final List<String> RESTRICTION_HINT_KEYWORDS = List.of(
            "kg", "이하", "미만", "소형", "중형", "대형", "입마개"
    );
    // "제한" 뒤에 "(사항)(은/이) 없" 형태가 바로 오면 "제한이 있다"는 뜻이 아니므로 힌트로 치지 않는다.
    private static final Pattern REAL_RESTRICTION_MENTION_PATTERN =
            Pattern.compile("제한(?!\\s*(?:사항)?\\s*(?:은|이)?\\s*없)");

    // "숫자 + kg/킬로(그램) + 이하/미만/까지/이내"만 다룬다. "초과"/"이상" 같은 반대 방향 조건이나 cm(체고)
    // 단위는 방향이 헷갈리기 쉽고(Pet에는 체중만 있고 체고 필드가 없음) 안전하게 판단하기 어려워 일부러
    // 다루지 않고 AI로 넘긴다.
    private static final Pattern WEIGHT_LIMIT_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:kg|킬로그램|킬로)\\s*(?:이하|미만|까지|이내)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SMALL_ONLY_PATTERN =
            Pattern.compile("소형견?\\s*(?:에\\s*한해|만|한정|까지|이하|이내)");
    private static final Pattern MEDIUM_LIMIT_PATTERN =
            Pattern.compile("중형견?\\s*(?:에\\s*한해|만|한정|까지|이하|이내)");

    // tryExtractStructured 전용 게이트. "불가"/"금지"가 조금이라도 섞여 있으면(다른 조항에 대한 예외여도)
    // 절대 규칙만으로 "허용"을 단정하지 않는다 - "일부는 불가할 수 있음" 같은 예외를 놓치는 게 제일 위험한 오탐.
    private static final List<String> ANY_DENIAL_HINT = List.of("불가", "금지");
    private static final List<String> POSITIVE_HINT = List.of("가능");

    // 아래 준비물-전용 패턴(무게/크기 제한 없이 "목줄만 챙기면 가능" 케이스)의 추가 게이트 - 2026-09-15,
    // Gemini 무료 티어 한도가 요청 몇 건 만에 소진되는 걸 실측으로 확인한 뒤 규칙 커버리지를 넓히면서 추가.
    // "문의"/"상이"/"제한"/"협의"가 섞여 있으면(예: "실내 시설은 개별 문의 필요", "매장별 정책이 상이") 그
    // 자체가 "케이스별로 다르다"는 신호라 규칙으로 단정하지 않고 AI로 넘긴다 - "불가"/"금지"만큼 확실한
    // 위험 신호는 아니지만, 이 단어들이 있는 원문은 실측상 전부 매장/구역별 예외가 있는 경우였다.
    private static final List<String> AMBIGUITY_HINT = List.of("문의", "상이", "제한", "협의");

    // TourAPI 반려동물 동반 조건 원문 실측(2026-09-15, 서울 지역 관광지 20건 샘플) - 거의 전 항목이 이
    // 어휘들로 "준비물"을 표현한다("목줄 착용", "배변봉투 지참" 등). 이 중 하나라도 있으면 "가능하지만
    // 준비물 필요"로 조건부 판정한다.
    private static final List<String> REQUIRED_ITEM_KEYWORDS = List.of(
            "목줄", "리드줄", "입마개", "배변봉투", "이동장", "켄넬", "유모차", "예방접종"
    );

    private RuleBasedConditionJudge() {
    }

    /** 아주 명확한 전면 허용/불가 표현만 즉시 판정한다. 애매하면 항상 비워서 다음 단계로 넘긴다. */
    static Optional<MatchResponse> judge(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.of(new MatchResponse(
                    MatchResponse.STATUS_UNKNOWN, "반려동물 동반 조건 정보가 등록되어 있지 않습니다.", rawText));
        }
        if (containsAny(rawText, DENY_KEYWORDS)) {
            return Optional.of(new MatchResponse(
                    MatchResponse.STATUS_DENIED, "반려동물 동반이 불가능한 곳입니다.", rawText));
        }
        if (containsAny(rawText, UNCONDITIONAL_ALLOW_KEYWORDS) && !hasRestrictionHint(rawText)) {
            return Optional.of(new MatchResponse(
                    MatchResponse.STATUS_ALLOWED, "모든 반려동물과 함께 이용 가능합니다.", rawText));
        }
        return Optional.empty();
    }

    private static boolean hasRestrictionHint(String text) {
        return containsAny(text, RESTRICTION_HINT_KEYWORDS) || REAL_RESTRICTION_MENTION_PATTERN.matcher(text).find();
    }

    /**
     * judge()가 못 잡은 경우 중, "체중/크기 상한이 있는 명확한 허용" 패턴만 추가로 시도한다.
     * 여기서 뽑히면 confidence=1.0인 ParsedCondition을 만들어서, AI가 뽑았을 때와 동일한 후처리
     * (Pet 체중/크기 대조 등)를 MatchingService가 그대로 적용한다. 뽑을 게 없거나 조금이라도 애매하면
     * Optional.empty()를 반환해 AI로 넘긴다.
     */
    static Optional<ParsedCondition> tryExtractStructured(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        if (containsAny(rawText, ANY_DENIAL_HINT) || !containsAny(rawText, POSITIVE_HINT)) {
            return Optional.empty();
        }

        Double maxWeightKg = null;
        Matcher weightMatcher = WEIGHT_LIMIT_PATTERN.matcher(rawText);
        if (weightMatcher.find()) {
            maxWeightKg = Double.parseDouble(weightMatcher.group(1));
        }

        String maxSizeCategory = null;
        if (SMALL_ONLY_PATTERN.matcher(rawText).find()) {
            maxSizeCategory = "SMALL";
        } else if (MEDIUM_LIMIT_PATTERN.matcher(rawText).find()) {
            maxSizeCategory = "MEDIUM";
        }

        if (maxWeightKg != null || maxSizeCategory != null) {
            return Optional.of(new ParsedCondition(
                    true, false, maxWeightKg, maxSizeCategory, "", restrictionText(maxWeightKg, maxSizeCategory), 1.0));
        }

        // 무게/크기 제한은 없고 "목줄 착용", "배변봉투 지참" 같은 준비물만 언급된, TourAPI에서 가장 흔한
        // 패턴이다(2026-09-15 추가). AMBIGUITY_HINT가 있으면(사례별로 다를 수 있다는 신호) 규칙으로
        // 단정하지 않고 AI로 넘긴다.
        if (containsAny(rawText, AMBIGUITY_HINT)) {
            return Optional.empty();
        }
        String requiredItems = REQUIRED_ITEM_KEYWORDS.stream()
                .filter(rawText::contains)
                .collect(Collectors.joining(", "));
        if (requiredItems.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedCondition(true, false, null, null, requiredItems, "", 1.0));
    }

    private static String restrictionText(Double maxWeightKg, String maxSizeCategory) {
        StringBuilder sb = new StringBuilder();
        if (maxWeightKg != null) {
            sb.append(String.format("체중 %.1fkg 이하", maxWeightKg));
        }
        if (maxSizeCategory != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("SMALL".equals(maxSizeCategory) ? "소형견 한정" : "중형견까지 가능");
        }
        return sb.toString();
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}
