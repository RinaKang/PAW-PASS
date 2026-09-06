package com.pawpass.user.service;

import com.pawpass.favorite.repository.FavoriteRepository;
import com.pawpass.pet.repository.PetRepository;
import com.pawpass.trip.repository.TripRepository;
import com.pawpass.user.domain.User;
import com.pawpass.user.dto.TravelConditionRequest;
import com.pawpass.user.dto.TravelConditionResponse;
import com.pawpass.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final TripRepository tripRepository;
    private final FavoriteRepository favoriteRepository;

    @Transactional
    public TravelConditionResponse updateTravelCondition(Long userId, TravelConditionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다: " + userId));
        user.updateTravelCondition(request.regionCode(), request.travelCategory());
        return TravelConditionResponse.from(user);
    }

    @Transactional
    public void withdraw(Long userId) {
        petRepository.deleteAllByUserId(userId);
        tripRepository.deleteAllByUserId(userId);
        favoriteRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }
}
