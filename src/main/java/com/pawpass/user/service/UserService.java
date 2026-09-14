package com.pawpass.user.service;

import com.pawpass.favorite.repository.FavoriteRepository;
import com.pawpass.pet.repository.PetRepository;
import com.pawpass.trip.repository.TripRepository;
import com.pawpass.user.domain.User;
import com.pawpass.user.dto.PrimaryPetResponse;
import com.pawpass.user.dto.TravelConditionRequest;
import com.pawpass.user.dto.TravelConditionResponse;
import com.pawpass.user.dto.UserResponse;
import com.pawpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final TripRepository tripRepository;
    private final FavoriteRepository favoriteRepository;
    private final ProfileImageStorage profileImageStorage;

    @Transactional
    public TravelConditionResponse updateTravelCondition(Long userId, TravelConditionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
        user.updateTravelCondition(request.regionCode(), request.travelCategory());
        return TravelConditionResponse.from(user);
    }

    /** 반려동물이 2마리 이상인 사용자가 대표 반려동물을 명시적으로 고르거나 바꿀 때 쓴다. */
    @Transactional
    public PrimaryPetResponse updatePrimaryPet(Long userId, Long petId) {
        petRepository.findByIdAndUserId(petId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다: " + petId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
        user.changePrimaryPet(petId);
        return new PrimaryPetResponse(petId);
    }

    /**
     * 기존 picture는 구글 로그인 시점에만 채워지던 필드(read-only)였는데, 여기서부턴 사용자가 직접 올린
     * 이미지로 덮어쓸 수 있다(2026-09-15 추가). 이전 이미지가 우리가 저장한 파일이면(구글 사진 URL이 아니라)
     * 교체 후 지운다 - 안 지우면 재업로드할 때마다 디스크에 계속 쌓인다.
     */
    @Transactional
    public UserResponse updateProfileImage(Long userId, MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
        String oldPicture = user.getPicture();
        String newPicture = profileImageStorage.store(image, userId);
        user.updatePicture(newPicture);
        profileImageStorage.deleteIfManaged(oldPicture);
        return UserResponse.from(user);
    }

    @Transactional
    public void withdraw(Long userId) {
        petRepository.deleteAllByUserId(userId);
        tripRepository.deleteAllByUserId(userId);
        favoriteRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }
}
