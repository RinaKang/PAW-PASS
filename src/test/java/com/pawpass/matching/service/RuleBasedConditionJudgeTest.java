package com.pawpass.matching.service;

import com.pawpass.matching.ai.PetConditionAiParser.ParsedCondition;
import com.pawpass.matching.dto.MatchResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedConditionJudgeTest {

    @Test
    void 빈_원문이면_확인필요() {
        Optional<MatchResponse> result = RuleBasedConditionJudge.judge("");

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(MatchResponse.STATUS_UNKNOWN);
    }

    @Test
    void 명시적_불가_키워드가_있으면_즉시_불가() {
        Optional<MatchResponse> result = RuleBasedConditionJudge.judge("본 시설은 반려동물 동반 불가 시설입니다.");

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(MatchResponse.STATUS_DENIED);
    }

    @Test
    void 제한없이_전견종_가능이면_즉시_가능() {
        Optional<MatchResponse> result = RuleBasedConditionJudge.judge("전 견종 가능하며 자유롭게 이용하실 수 있습니다.");

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(MatchResponse.STATUS_ALLOWED);
    }

    @Test
    void 크기_제한_힌트가_섞여있으면_규칙으로_단정하지_않고_AI로_넘긴다() {
        Optional<MatchResponse> result = RuleBasedConditionJudge.judge("전 견종 가능하나 5kg 이하만 동반 가능합니다.");

        assertThat(result).isEmpty();
    }

    @Test
    void 애매한_원문은_AI로_넘긴다() {
        Optional<MatchResponse> result = RuleBasedConditionJudge.judge("실내는 케이지 동반 시에만 이용 가능합니다.");

        assertThat(result).isEmpty();
    }

    @Test
    void 숫자_체중_상한을_정확히_추출한다() {
        Optional<ParsedCondition> result = RuleBasedConditionJudge.tryExtractStructured("5kg 이하 소형견만 동반 가능합니다.");

        assertThat(result).isPresent();
        assertThat(result.get().maxWeightKg()).isEqualTo(5.0);
        assertThat(result.get().maxSizeCategory()).isEqualTo("SMALL");
        assertThat(result.get().explicitlyAllowed()).isTrue();
        assertThat(result.get().confidence()).isEqualTo(1.0);
    }

    @Test
    void 소형견만_한정_까지_등_다양한_표현을_인식한다() {
        assertThat(RuleBasedConditionJudge.tryExtractStructured("소형견에 한해 동반 가능합니다.").get().maxSizeCategory())
                .isEqualTo("SMALL");
        assertThat(RuleBasedConditionJudge.tryExtractStructured("소형견만 가능해요.").get().maxSizeCategory())
                .isEqualTo("SMALL");
        assertThat(RuleBasedConditionJudge.tryExtractStructured("중형견까지 가능합니다.").get().maxSizeCategory())
                .isEqualTo("MEDIUM");
    }

    @Test
    void 불가_금지가_조금이라도_섞여있으면_규칙으로_추출하지_않는다() {
        Optional<ParsedCondition> result = RuleBasedConditionJudge.tryExtractStructured(
                "소형견만 가능하나 식음료 매장 입장은 불가할 수 있습니다.");

        assertThat(result).isEmpty();
    }

    @Test
    void 가능_이라는_단어가_없으면_추출하지_않는다() {
        Optional<ParsedCondition> result = RuleBasedConditionJudge.tryExtractStructured("소형견 5kg 이하");

        assertThat(result).isEmpty();
    }

    @Test
    void 체중_이상이나_초과처럼_반대_방향_조건은_추출하지_않는다() {
        Optional<ParsedCondition> result = RuleBasedConditionJudge.tryExtractStructured("5kg 이상만 동반 가능합니다.");

        assertThat(result).isEmpty();
    }

    @Test
    void 체중_크기_언급이_전혀_없으면_추출하지_않는다() {
        Optional<ParsedCondition> result = RuleBasedConditionJudge.tryExtractStructured("실내에서만 동반 가능합니다.");

        assertThat(result).isEmpty();
    }

    // KCISA 실제 DB 데이터(pet_facilities)를 확인해보니 "모두 가능" + "제한사항 없음" 조합이 압도적으로
    // 흔했다(2026-09-10) - 이 케이스를 규칙만으로 잡는 게 비용 절감 효과가 가장 크다.
    @Test
    void 실데이터에서_가장_흔한_모두_가능_제한사항_없음_조합을_즉시_가능으로_판정한다() {
        Optional<MatchResponse> result = RuleBasedConditionJudge.judge("모두 가능\n제한사항 없음");

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(MatchResponse.STATUS_ALLOWED);
    }

    @Test
    void 제한사항_없음은_제한_힌트로_취급하지_않지만_진짜_제한_언급은_여전히_AI로_넘긴다() {
        assertThat(RuleBasedConditionJudge.judge("모두 가능\n제한사항 없음")).isPresent();
        assertThat(RuleBasedConditionJudge.judge("모두 가능\n체중 제한이 있습니다")).isEmpty();
    }

    // 2026-09-15: Gemini 무료 티어 한도가 /explore 한 번에 거의 소진되는 걸 실측으로 확인한 뒤(관광지
    // 20건 중 18건이 "판정 시도조차 못 하고" 확인필요로 대체됨) 규칙 커버리지를 넓힘. 실제 TourAPI
    // 응답(서울 지역 관광지, 2026-09-15 실측)을 그대로 가져와 테스트한다 - 무게/크기 제한 없이
    // "목줄 착용" 같은 준비물만 언급된 경우가 압도적으로 흔한 패턴이었다.
    @Test
    void 무게_크기_제한_없이_준비물만_언급되면_조건부_허용으로_추출한다() {
        // 실측 원문(노들섬, 2026-09-15)
        String rawText = "전구역 동반가능\n전 견종 동반 가능\n목줄 착용\n"
                + "- 맹견의 경우, 입마개 착용 필수- 배변봉투 지참 및 배변처리 필수";

        Optional<ParsedCondition> result = RuleBasedConditionJudge.tryExtractStructured(rawText);

        assertThat(result).isPresent();
        assertThat(result.get().explicitlyAllowed()).isTrue();
        assertThat(result.get().maxWeightKg()).isNull();
        assertThat(result.get().maxSizeCategory()).isNull();
        assertThat(result.get().requiredItems()).contains("목줄", "입마개", "배변봉투");
        assertThat(result.get().confidence()).isEqualTo(1.0);
    }

    // "개별 문의 필요"/"정책이 상이"처럼 사례별로 달라질 수 있다는 힌트가 있으면, 겉보기엔 "동반가능"
    // 문구가 있어도 규칙으로 단정하지 않고 AI로 넘긴다 - 실측 원문(북촌 8경/세빛섬, 2026-09-15).
    @Test
    void 문의나_상이_등_사례별_예외_힌트가_있으면_준비물_패턴이어도_AI로_넘긴다() {
        String bukchon = "일부구역 동반가능\n전 견종 동반 가능\n목줄 착용\n"
                + "- 실내 시설 입장은 개별 문의 필요- 맹견의 경우, 입마개 착용 필수- 배변봉투 지참 및 배변처리 필수";
        String saevit = "일부구역 동반가능\n전 견종 동반 가능\n목줄 착용\n"
                + "- 매장별 정책이 상이하므로 동반 가능 여부 개별 문의 필요";

        assertThat(RuleBasedConditionJudge.tryExtractStructured(bukchon)).isEmpty();
        assertThat(RuleBasedConditionJudge.tryExtractStructured(saevit)).isEmpty();
    }

    // "일부 구역은 동반 금지"처럼 부분 제외가 있는 경우 - 기존 ANY_DENIAL_HINT 게이트가 그대로 막아준다
    // (실측 원문: 월드컵공원, 2026-09-15). 준비물 패턴 추가가 이 안전장치를 우회하면 안 된다.
    @Test
    void 일부_구역_동반_금지가_섞여있으면_준비물_패턴이어도_여전히_AI로_넘긴다() {
        String worldcup = "일부구역 동반가능\n전 견종 동반 가능\n목줄 착용\n"
                + "- 유아·어린이 놀이터, 노을캠핑장 등은 동반 금지- 맹견의 경우, 입마개 착용 필수";

        assertThat(RuleBasedConditionJudge.tryExtractStructured(worldcup)).isEmpty();
    }

    // 2026-09-19: "카페 카테고리에 가능만 뜨고 조건부/불가는 하나도 안 뜬다"는 리포트로 발견 - KCISA
    // allowed_pet_size 필드 실측 원문(staywell cafe, 2026-09-19)은 "소형견만 동반 가능합니다" 같은 문장이
    // 아니라 "소형" 한 단어뿐이라 "가능"이 원문에 아예 없다. 기존 게이트(POSITIVE_HINT 요구)에 걸려 전부
    // AI로 넘어가고 있었는데, 지금 Gemini 한도 소진으로 대부분 확인필요에 눌러앉아 있었다.
    @Test
    void KCISA_소형_단독_값은_가능_단어가_없어도_조건부로_추출한다() {
        String rawText = "제한사항 없음\n소형\n해당없음\n없음";

        Optional<ParsedCondition> result = RuleBasedConditionJudge.tryExtractStructured(rawText);

        assertThat(result).isPresent();
        assertThat(result.get().explicitlyAllowed()).isTrue();
        assertThat(result.get().maxSizeCategory()).isEqualTo("SMALL");
        assertThat(result.get().confidence()).isEqualTo(1.0);
    }

    @Test
    void KCISA_중형_대형_단독_값도_각각_인식한다() {
        assertThat(RuleBasedConditionJudge.tryExtractStructured("제한사항 없음\n중형\n해당없음\n없음")
                .get().maxSizeCategory()).isEqualTo("MEDIUM");
        assertThat(RuleBasedConditionJudge.tryExtractStructured("제한사항 없음\n대형\n해당없음\n없음")
                .get().maxSizeCategory()).isEqualTo("LARGE");
    }

    // "소형 제외"처럼 그 줄에 다른 말이 붙으면 애매한 뜻이 될 수 있으니(제외 대상인지 허용 대상인지) 단독
    // 줄로 딱 떨어질 때만 매치해야 한다 - 붙어있으면 여전히 AI로 넘어가야 정상.
    @Test
    void 크기_단어에_다른_말이_붙어있으면_단독_값으로_인식하지_않는다() {
        assertThat(RuleBasedConditionJudge.tryExtractStructured("제한사항 없음\n소형 제외\n해당없음\n없음")).isEmpty();
    }

    @Test
    void 크기_단독_값이어도_불가_금지가_섞여있으면_여전히_AI로_넘긴다() {
        assertThat(RuleBasedConditionJudge.tryExtractStructured("소형\n단, 일부 매장은 동반 불가")).isEmpty();
    }
}
