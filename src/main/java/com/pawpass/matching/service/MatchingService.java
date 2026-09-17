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
import com.pawpass.user.domain.User;
import com.pawpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    /** 다견 AND 판정에서 하나라도 이 순서로 걸리면 그 상태가 전체 결과를 결정한다(최악 우선). */
    private static final List<String> STATUS_PRIORITY = List.of(
            MatchResponse.STATUS_DENIED, MatchResponse.STATUS_UNKNOWN, MatchResponse.STATUS_CONDITIONAL
    );

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final TourService tourService;
    private final FacilityService facilityService;
    private final PetConditionAiParser petConditionAiParser;

    public MatchResponse matchTour(Long userId, String contentId, Long petId) {
        return matchTour(userId, contentId, petId == null ? List.of() : List.of(petId));
    }

    /** petIds가 2개 이상이면(2026-09-19 추가, 다견 AND 판정) 전체가 함께 이용 가능한지를 판정한다. */
    public MatchResponse matchTour(Long userId, String contentId, List<Long> petIds) {
        List<Pet> pets = requirePetsForMatch(userId, petIds);
        return matchTourForPets(pets, contentId);
    }

    public MatchResponse matchFacility(Long userId, String id, Long petId) {
        return matchFacility(userId, id, petId == null ? List.of() : List.of(petId));
    }

    public MatchResponse matchFacility(Long userId, String id, List<Long> petIds) {
        List<Pet> pets = requirePetsForMatch(userId, petIds);
        return matchFacilityForPets(pets, id);
    }

    /**
     * /explore 목록처럼 이미 소유권이 확인된 Pet 하나를 여러 항목에 반복 적용할 때 쓴다
     * (항목마다 DB에서 Pet을 다시 조회하지 않도록 소유권 확인과 판정을 분리).
     */
    public MatchResponse matchTourForPet(Pet pet, String contentId) {
        return matchTourForPets(List.of(pet), contentId);
    }

    public MatchResponse matchTourForPets(List<Pet> pets, String contentId) {
        TourDetailResponse detail = tourService.getDetail(contentId);
        String rawText = joinNonBlank(
                detail.petCondition().acmpyTypeCd(),
                detail.petCondition().acmpyPsblCpam(),
                detail.petCondition().acmpyNeedMtr(),
                detail.petCondition().etcAcmpyInfo()
        );
        return judge(rawText, pets);
    }

    public MatchResponse matchFacilityForPet(Pet pet, String id) {
        return matchFacilityForPets(List.of(pet), id);
    }

    public MatchResponse matchFacilityForPets(List<Pet> pets, String id) {
        FacilityDetailResponse.PetCondition condition = facilityService.getDetail(id).petCondition();
        String rawText = joinNonBlank(
                condition.petRestriction(),
                condition.allowedPetSize(),
                condition.petExclusive(),
                condition.additionalPetFee()
        );
        return judge(rawText, pets);
    }

    public Pet requireOwnedPet(Long userId, Long petId) {
        return petRepository.findByIdAndUserId(petId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다: " + petId));
    }

    /**
     * /tours,/facilities의 {id}/match용 - petId를 안 넘기면 대표 반려동물(User.primaryPetId)로 대체한다.
     * 대표 반려동물도 없으면(반려동물이 아예 없거나, 2마리 이상인데 아직 하나를 안 골랐거나) 명확한 에러로
     * "먼저 선택해주세요"를 안내한다 - 매칭은 특정 펫 없이는 의미가 없는 기능이라 조용히 넘어가지 않는다.
     */
    public Pet requirePetForMatch(Long userId, Long petId) {
        return requirePetsForMatch(userId, petId == null ? List.of() : List.of(petId)).get(0);
    }

    /** petIds가 비어 있으면 petId 없을 때와 동일하게 대표 반려동물 1마리로 대체한다. */
    public List<Pet> requirePetsForMatch(Long userId, List<Long> petIds) {
        if (petIds != null && !petIds.isEmpty()) {
            return petIds.stream().map(petId -> requireOwnedPet(userId, petId)).toList();
        }
        Long primaryPetId = findPrimaryPetId(userId);
        if (primaryPetId == null) {
            throw new IllegalArgumentException("매칭할 반려동물을 먼저 선택해주세요.");
        }
        return List.of(requireOwnedPet(userId, primaryPetId));
    }

    /**
     * /explore용 - petId도 대표 반려동물도 없으면 조용히 null을 반환해 개인화 없이 진행하게 한다
     * (목록 조회는 매칭 없이도 의미가 있는 기능이라 requirePetForMatch처럼 에러를 던지지 않는다).
     * userId가 null이면(비로그인 브라우징) 바로 null - 대표 반려동물 조회 자체를 시도하지 않는다.
     */
    public Pet resolveOptionalPet(Long userId, Long petId) {
        List<Pet> pets = resolveOptionalPets(userId, petId == null ? List.of() : List.of(petId));
        return pets.isEmpty() ? null : pets.get(0);
    }

    public List<Pet> resolveOptionalPets(Long userId, List<Long> petIds) {
        if (userId == null) {
            return List.of();
        }
        if (petIds != null && !petIds.isEmpty()) {
            return petIds.stream().map(petId -> requireOwnedPet(userId, petId)).toList();
        }
        Long primaryPetId = findPrimaryPetId(userId);
        return primaryPetId == null ? List.of() : List.of(requireOwnedPet(userId, primaryPetId));
    }

    private Long findPrimaryPetId(Long userId) {
        return userRepository.findById(userId).map(User::getPrimaryPetId).orElse(null);
    }

    private MatchResponse judge(String rawText, List<Pet> pets) {
        Optional<MatchResponse> immediate = RuleBasedConditionJudge.judge(rawText);
        if (immediate.isPresent()) {
            // 확인필요(원문 없음)/전면 불가/무조건 가능은 펫의 몸무게·크기와 무관하게 결정되는 판정이라
            // 선택된 펫이 몇 마리든 동일하게 적용된다 - 펫별로 다시 계산할 필요가 없다.
            return immediate.get();
        }
        ParsedCondition parsed = RuleBasedConditionJudge.tryExtractStructured(rawText)
                .orElseGet(() -> petConditionAiParser.parse(rawText));
        List<MatchResponse> perPet = pets.stream().map(pet -> toMatchResponse(parsed, pet, rawText)).toList();
        return combine(perPet, pets, rawText);
    }

    /**
     * 펫이 1마리면 기존 단일 펫 판정과 완전히 동일한 결과를 그대로 반환한다(하위 호환).
     * 2마리 이상이면 전체가 함께 이용 가능해야 하므로 AND로 합친다 - 가장 제한적인 상태(불가 > 확인필요 >
     * 조건부 > 가능 순)가 전체 결과가 되고, 그 원인이 된 반려동물 이름을 사유 앞에 붙인다.
     */
    private MatchResponse combine(List<MatchResponse> perPet, List<Pet> pets, String rawText) {
        if (perPet.size() == 1) {
            return perPet.get(0);
        }
        for (String status : STATUS_PRIORITY) {
            for (int i = 0; i < perPet.size(); i++) {
                if (status.equals(perPet.get(i).status())) {
                    return new MatchResponse(status, pets.get(i).getName() + ": " + perPet.get(i).reason(), rawText);
                }
            }
        }
        return new MatchResponse(MatchResponse.STATUS_ALLOWED, "선택하신 반려동물 모두 함께 이용 가능합니다.", rawText);
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
