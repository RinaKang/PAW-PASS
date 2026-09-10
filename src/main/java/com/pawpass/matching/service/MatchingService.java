package com.pawpass.matching.service;

import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.service.FacilityService;
import com.pawpass.matching.ai.PetConditionAiParser;
import com.pawpass.matching.ai.PetConditionAiParser.ParsedCondition;
import com.pawpass.matching.dto.MatchResponse;
import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.domain.PetSize;
import com.pawpass.pet.repository.PetRepository;
import com.pawpass.tour.dto.TourDetailResponse;
import com.pawpass.tour.service.TourService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 개인별 방문 가능 여부 매칭. {@link RuleBasedConditionJudge}가 두 단계로 최대한 걸러내고
 * (① 아주 명확한 전면 허용/불가, ② "숫자+kg+이하"/"소형견만" 같은 정형화된 체중·크기 제한),
 * 그래도 애매한 나머지만 {@link PetConditionAiParser}(Gemini)로 넘기는 하이브리드 구조 -
 * Gemini 무료 티어 분당 호출 한도 때문에 비용/레이트리밋을 줄이려고 ②를 나중에 추가함.
 * 결과가 규칙에서 나왔든 AI에서 나왔든 동일하게, 뽑아낸 체중/크기 상한(maxWeightKg/maxSizeCategory)을
 * 실제 Pet과 숫자로 대조해서 최종 판정에 반영한다 - "소형견 한정" 같은 제한도 텍스트 안내로 끝나지 않고
 * 확정적으로 불가 처리된다.
 */
@Service
@RequiredArgsConstructor
public class MatchingService {

    private static final double CONFIDENCE_THRESHOLD = 0.5;

    private final PetRepository petRepository;
    private final TourService tourService;
    private final FacilityService facilityService;
    private final PetConditionAiParser petConditionAiParser;

    public MatchResponse matchTour(Long userId, String contentId, Long petId) {
        Pet pet = requireOwnedPet(userId, petId);
        return matchTourForPet(pet, contentId);
    }

    public MatchResponse matchFacility(Long userId, String id, Long petId) {
        Pet pet = requireOwnedPet(userId, petId);
        return matchFacilityForPet(pet, id);
    }

    /**
     * /explore 목록처럼 이미 소유권이 확인된 Pet 하나를 여러 항목에 반복 적용할 때 쓴다
     * (항목마다 DB에서 Pet을 다시 조회하지 않도록 소유권 확인과 판정을 분리).
     */
    public MatchResponse matchTourForPet(Pet pet, String contentId) {
        TourDetailResponse detail = tourService.getDetail(contentId);
        String rawText = joinNonBlank(
                detail.petCondition().acmpyTypeCd(),
                detail.petCondition().acmpyPsblCpam(),
                detail.petCondition().acmpyNeedMtr(),
                detail.petCondition().etcAcmpyInfo()
        );
        return judge(rawText, pet);
    }

    public MatchResponse matchFacilityForPet(Pet pet, String id) {
        FacilityDetailResponse.PetCondition condition = facilityService.getDetail(id).petCondition();
        String rawText = joinNonBlank(
                condition.petRestriction(),
                condition.allowedPetSize(),
                condition.petExclusive(),
                condition.additionalPetFee()
        );
        return judge(rawText, pet);
    }

    public Pet requireOwnedPet(Long userId, Long petId) {
        return petRepository.findByIdAndUserId(petId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다: " + petId));
    }

    private MatchResponse judge(String rawText, Pet pet) {
        Optional<MatchResponse> immediate = RuleBasedConditionJudge.judge(rawText);
        if (immediate.isPresent()) {
            return immediate.get();
        }
        ParsedCondition parsed = RuleBasedConditionJudge.tryExtractStructured(rawText)
                .orElseGet(() -> petConditionAiParser.parse(rawText));
        return toMatchResponse(parsed, pet, rawText);
    }

    /** parsed가 규칙 기반 추출이든 AI든 상관없이 동일한 후처리(신뢰도/크기 대조/허용-불가 분기)를 적용한다. */
    private MatchResponse toMatchResponse(ParsedCondition parsed, Pet pet, String rawText) {
        if (parsed.confidence() < CONFIDENCE_THRESHOLD) {
            return new MatchResponse(MatchResponse.STATUS_UNKNOWN, "AI 판정 신뢰도가 낮아 직접 확인이 필요합니다.", rawText);
        }

        String sizeViolation = petExceedsSizeLimit(pet, parsed);
        if (sizeViolation != null) {
            return new MatchResponse(MatchResponse.STATUS_DENIED, sizeViolation, rawText);
        }
        if (parsed.explicitlyDenied()) {
            return new MatchResponse(MatchResponse.STATUS_DENIED, reasonOrDefault(parsed, "반려동물 동반이 불가능합니다."), rawText);
        }
        if (parsed.explicitlyAllowed()) {
            if (hasText(parsed.restrictions()) || hasText(parsed.requiredItems())) {
                return new MatchResponse(MatchResponse.STATUS_CONDITIONAL, reasonOrDefault(parsed, "조건부로 동반이 가능합니다."), rawText);
            }
            return new MatchResponse(MatchResponse.STATUS_ALLOWED, "반려동물과 함께 이용 가능합니다.", rawText);
        }
        return new MatchResponse(MatchResponse.STATUS_UNKNOWN, "원문만으로는 명확히 판단하기 어렵습니다. 직접 확인해주세요.", rawText);
    }

    /**
     * 원문에서 뽑아낸 체중/크기 상한을 실제 Pet과 대조한다. 하나라도 기준을 넘으면, AI가 explicitlyAllowed로
     * 판정했더라도 이 결과가 우선해서 확정적으로 불가 처리한다 - 텍스트로만 안내하고 마는 게 아니라 실제로
     * 걸러내는 게 이번에 추가한 핵심 동작이다.
     */
    private String petExceedsSizeLimit(Pet pet, ParsedCondition parsed) {
        if (parsed.maxWeightKg() != null && pet.getWeight() != null && pet.getWeight() > parsed.maxWeightKg()) {
            return String.format("%.1fkg 이하만 동반 가능한 곳인데, %s(%.1fkg)은(는) 기준을 초과합니다.",
                    parsed.maxWeightKg(), pet.getName(), pet.getWeight());
        }
        PetSize maxAllowed = parseSize(parsed.maxSizeCategory());
        if (maxAllowed != null && pet.getSize() != null && pet.getSize().ordinal() > maxAllowed.ordinal()) {
            return String.format("%s까지만 동반 가능한 곳인데, %s(%s)은(는) 기준을 초과합니다.",
                    sizeLabel(maxAllowed), pet.getName(), sizeLabel(pet.getSize()));
        }
        return null;
    }

    /** Gemini가 스키마(enum)를 벗어난 값을 보낼 가능성에 대비한 방어적 파싱 - 벗어나면 제한 없음으로 취급 */
    private PetSize parseSize(String value) {
        if (value == null) {
            return null;
        }
        try {
            return PetSize.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String sizeLabel(PetSize size) {
        return switch (size) {
            case SMALL -> "소형견";
            case MEDIUM -> "중형견";
            case LARGE -> "대형견";
        };
    }

    private String reasonOrDefault(ParsedCondition parsed, String fallback) {
        StringBuilder reason = new StringBuilder();
        if (hasText(parsed.restrictions())) {
            reason.append(parsed.restrictions());
        }
        if (hasText(parsed.requiredItems())) {
            if (reason.length() > 0) {
                reason.append(" / ");
            }
            reason.append("준비물: ").append(parsed.requiredItems());
        }
        return reason.length() > 0 ? reason.toString() : fallback;
    }

    private String joinNonBlank(String... parts) {
        return Stream.of(parts).filter(this::hasText).collect(Collectors.joining("\n"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
