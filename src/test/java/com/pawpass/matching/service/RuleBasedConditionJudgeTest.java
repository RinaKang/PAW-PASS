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
}
