package com.pawpass.pet.service;

import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.dto.PetRequest;
import com.pawpass.pet.dto.PetResponse;
import com.pawpass.pet.repository.PetRepository;
import com.pawpass.user.domain.User;
import com.pawpass.user.repository.UserRepository;
import com.pawpass.user.service.ProfileImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private static final String PET_IMAGE_CATEGORY = "pet-images";

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final ProfileImageStorage profileImageStorage;

    /**
     * 반려동물이 1마리뿐이면(이번에 등록한 게 처음이자 유일한 반려동물이면) 자동으로 대표 반려동물로
     * 지정한다(2026-09-13) - 2마리 이상이면 어떤 걸 기본으로 쓸지 모호해서 자동 지정 안 하고 사용자가
     * PUT /users/me/primary-pet으로 명시적으로 골라야 한다.
     */
    @Transactional
    public PetResponse create(Long userId, PetRequest request) {
        Pet pet = Pet.builder()
                .userId(userId)
                .name(request.name())
                .species(request.species())
                .breed(request.breed())
                .weight(request.weight())
                .size(request.size())
                .hasCarrier(Boolean.TRUE.equals(request.hasCarrier()))
                .hasLeash(Boolean.TRUE.equals(request.hasLeash()))
                .hasMuzzle(Boolean.TRUE.equals(request.hasMuzzle()))
                .hasWasteBags(Boolean.TRUE.equals(request.hasWasteBags()))
                .hasStroller(Boolean.TRUE.equals(request.hasStroller()))
                .hasDiaper(Boolean.TRUE.equals(request.hasDiaper()))
                .build();
        pet = petRepository.save(pet);

        boolean isOnlyPet = petRepository.findAllByUserId(userId).size() == 1;
        if (isOnlyPet) {
            User user = requireUser(userId);
            user.changePrimaryPet(pet.getId());
        }
        return PetResponse.from(pet, isOnlyPet);
    }

    public List<PetResponse> findAllByUser(Long userId) {
        Long primaryPetId = requireUser(userId).getPrimaryPetId();
        return petRepository.findAllByUserId(userId).stream()
                .map(pet -> PetResponse.from(pet, pet.getId().equals(primaryPetId)))
                .toList();
    }

    @Transactional
    public PetResponse update(Long userId, Long id, PetRequest request) {
        Pet pet = petRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다: " + id));
        pet.update(request.name(), request.species(), request.breed(), request.weight(), request.size(),
                request.hasCarrier(), request.hasLeash(), request.hasMuzzle(), request.hasWasteBags(),
                request.hasStroller(), request.hasDiaper());
        Long primaryPetId = requireUser(userId).getPrimaryPetId();
        return PetResponse.from(pet, id.equals(primaryPetId));
    }

    /**
     * 삭제한 반려동물이 대표였으면 대표 지정을 해제하고, 남은 반려동물이 정확히 1마리면 그 하나로
     * 자동 재지정한다(등록 시 자동 지정과 같은 규칙) - 2마리 이상 남으면 사용자가 다시 골라야 한다.
     */
    @Transactional
    public void delete(Long userId, Long id) {
        Pet pet = petRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다: " + id));
        petRepository.delete(pet);

        User user = requireUser(userId);
        if (id.equals(user.getPrimaryPetId())) {
            List<Pet> remaining = petRepository.findAllByUserId(userId);
            user.changePrimaryPet(remaining.size() == 1 ? remaining.get(0).getId() : null);
        }
    }

    /**
     * 소유권 확인(petRepository.findByIdAndUserId) 후 저장하고 이미지 URL을 갱신한다 - 사용자 프로필
     * 이미지(UserService.updateProfileImage)와 같은 흐름, 대상만 Pet일 뿐이다(2026-09-17 추가).
     */
    @Transactional
    public PetResponse updateProfileImage(Long userId, Long id, MultipartFile image) {
        Pet pet = petRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다: " + id));
        String oldImageUrl = pet.getImageUrl();
        String newImageUrl = profileImageStorage.store(image, PET_IMAGE_CATEGORY, id);
        pet.updateImage(newImageUrl);
        profileImageStorage.deleteIfManaged(oldImageUrl, PET_IMAGE_CATEGORY);
        Long primaryPetId = requireUser(userId).getPrimaryPetId();
        return PetResponse.from(pet, id.equals(primaryPetId));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
    }
}
