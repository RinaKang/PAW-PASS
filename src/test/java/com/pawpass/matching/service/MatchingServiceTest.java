package com.pawpass.matching.service;

import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.service.FacilityService;
import com.pawpass.matching.ai.PetConditionAiParser;
import com.pawpass.matching.dto.MatchResponse;
import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.domain.PetSize;
import com.pawpass.pet.repository.PetRepository;
import com.pawpass.tour.dto.TourDetailResponse;
import com.pawpass.tour.service.TourService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private PetRepository petRepository;
    @Mock
    private TourService tourService;
    @Mock
    private FacilityService facilityService;
    @Mock
    private PetConditionAiParser petConditionAiParser;

    @InjectMocks
    private MatchingService matchingService;

    private static final Pet OWNED_PET = Pet.builder()
            .userId(1L).name("초코").species("강아지").breed("말티즈").weight(3.0).size(PetSize.SMALL)
            .hasCarrier(true).hasLeash(true).build();

    private static final Pet LARGE_PET = Pet.builder()
            .userId(1L).name("바둑이").species("강아지").breed("리트리버").weight(30.0).size(PetSize.LARGE)
            .hasCarrier(true).hasLeash(true).build();

    @Test
    void 본인_소유가_아닌_반려동물이면_예외() {
        when(petRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.matchTour(1L, "123", 99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 규칙으로_명확히_판정되면_AI를_호출하지_않는다() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET));
        when(tourService.getDetail("123")).thenReturn(tourDetail("반려동물 동반 불가 시설입니다."));

        MatchResponse result = matchingService.matchTour(1L, "123", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_DENIED);
        verify(petConditionAiParser, never()).parse(anyString());
    }

    @Test
    void AI_신뢰도가_낮으면_확인필요() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET));
        when(tourService.getDetail("123")).thenReturn(tourDetail("실내는 케이지 동반 시에만 이용 가능합니다."));
        when(petConditionAiParser.parse(anyString()))
                .thenReturn(new PetConditionAiParser.ParsedCondition(true, false, null, null, "", "", 0.3));

        MatchResponse result = matchingService.matchTour(1L, "123", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_UNKNOWN);
    }

    @Test
    void AI가_명시적_허용이면서_제한이_있으면_조건부() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET));
        when(tourService.getDetail("123")).thenReturn(tourDetail("실내는 케이지 동반 시에만 이용 가능합니다."));
        when(petConditionAiParser.parse(anyString()))
                .thenReturn(new PetConditionAiParser.ParsedCondition(true, false, null, null, "이동장", "소형견만", 0.9));

        MatchResponse result = matchingService.matchTour(1L, "123", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_CONDITIONAL);
        assertThat(result.reason()).contains("소형견만").contains("이동장");
    }

    @Test
    void AI가_명시적_허용이고_제한이_없으면_가능() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET));
        when(tourService.getDetail("123")).thenReturn(tourDetail("실내는 케이지 동반 시에만 이용 가능합니다."));
        when(petConditionAiParser.parse(anyString()))
                .thenReturn(new PetConditionAiParser.ParsedCondition(true, false, null, null, "", "", 0.9));

        MatchResponse result = matchingService.matchTour(1L, "123", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_ALLOWED);
    }

    @Test
    void 체중_상한을_초과하면_명시적_허용이어도_불가() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET)); // weight=3.0kg
        when(tourService.getDetail("123")).thenReturn(tourDetail("체중 제한이 있습니다."));
        when(petConditionAiParser.parse(anyString()))
                .thenReturn(new PetConditionAiParser.ParsedCondition(true, false, 2.0, null, "", "2kg 이하만 가능", 0.9));

        MatchResponse result = matchingService.matchTour(1L, "123", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_DENIED);
        assertThat(result.reason()).contains("2.0kg").contains("초코");
    }

    @Test
    void 체중이_상한_이내면_기존_로직대로_판정된다() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET)); // weight=3.0kg
        when(tourService.getDetail("123")).thenReturn(tourDetail("체중 제한이 있습니다."));
        when(petConditionAiParser.parse(anyString()))
                .thenReturn(new PetConditionAiParser.ParsedCondition(true, false, 10.0, null, "", "", 0.9));

        MatchResponse result = matchingService.matchTour(1L, "123", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_ALLOWED);
    }

    // 규칙 기반(RuleBasedConditionJudge)이 못 잡도록 "가능"/"소형" 등 정형화된 단어를 일부러 피한 문장 -
    // AI 경로 + Pet 크기 대조 통합을 테스트하려는 목적이라 실제로 AI가 호출돼야 의미가 있다.
    @Test
    void 크기_분류_상한을_초과하면_불가_AI경로() {
        when(petRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(LARGE_PET));
        when(tourService.getDetail("123")).thenReturn(tourDetail("작은 강아지들만 함께하실 수 있어요."));
        when(petConditionAiParser.parse(anyString()))
                .thenReturn(new PetConditionAiParser.ParsedCondition(true, false, null, "SMALL", "", "소형견만 가능", 0.9));

        MatchResponse result = matchingService.matchTour(1L, "123", 2L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_DENIED);
        assertThat(result.reason()).contains("소형견").contains("대형견");
    }

    // 이번엔 반대로 규칙만으로 잡히는 정형 표현 - AI를 아예 호출하지 않아야 한다 (비용 절감이 핵심 요구사항)
    @Test
    void 소형견만_가능_처럼_정형화된_문장은_규칙만으로_판정하고_AI를_부르지_않는다() {
        when(petRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(LARGE_PET));
        when(tourService.getDetail("123")).thenReturn(tourDetail("소형견만 가능합니다."));

        MatchResponse result = matchingService.matchTour(1L, "123", 2L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_DENIED);
        verify(petConditionAiParser, never()).parse(anyString());
    }

    @Test
    void 체중_숫자_제한도_규칙만으로_판정하고_AI를_부르지_않는다() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET)); // weight=3.0kg
        when(tourService.getDetail("123")).thenReturn(tourDetail("2kg 이하 소형견만 가능합니다."));

        MatchResponse result = matchingService.matchTour(1L, "123", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_DENIED);
        assertThat(result.reason()).contains("2.0kg");
        verify(petConditionAiParser, never()).parse(anyString());
    }

    @Test
    void 크기_분류_기준_이내면_조건부로_통과한다_AI경로() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET)); // SMALL
        when(tourService.getDetail("123")).thenReturn(tourDetail("중형 크기까지는 함께하실 수 있어요."));
        when(petConditionAiParser.parse(anyString()))
                .thenReturn(new PetConditionAiParser.ParsedCondition(true, false, null, "MEDIUM", "", "중형견까지 가능", 0.9));

        MatchResponse result = matchingService.matchTour(1L, "123", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_CONDITIONAL);
    }

    @Test
    void 중형견까지_가능_처럼_정형화된_문장도_규칙만으로_판정한다() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET)); // SMALL
        when(tourService.getDetail("123")).thenReturn(tourDetail("중형견까지 가능합니다."));

        MatchResponse result = matchingService.matchTour(1L, "123", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_CONDITIONAL);
        verify(petConditionAiParser, never()).parse(anyString());
    }

    @Test
    void facility_매칭도_조건_텍스트를_모아_판정한다() {
        when(petRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(OWNED_PET));
        when(facilityService.getDetail("f1")).thenReturn(facilityDetail("반려동물 동반 불가"));

        MatchResponse result = matchingService.matchFacility(1L, "f1", 1L);

        assertThat(result.status()).isEqualTo(MatchResponse.STATUS_DENIED);
    }

    private TourDetailResponse tourDetail(String etcAcmpyInfo) {
        return new TourDetailResponse(
                "123", "제목", "주소", "tel", "hours", java.util.List.of(),
                new TourDetailResponse.PetCondition(null, null, null, etcAcmpyInfo),
                "issued");
    }

    private FacilityDetailResponse facilityDetail(String petRestriction) {
        return new FacilityDetailResponse(
                "제목", "주소", "tel", "hours",
                new FacilityDetailResponse.PetCondition(null, petRestriction, null, null, null, null),
                null);
    }
}
