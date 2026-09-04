package com.pawpass.pet.service;

import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.dto.PetRequest;
import com.pawpass.pet.dto.PetResponse;
import com.pawpass.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;

    // TODO: 인증 붙으면 파라미터로 userId 받도록 시그니처 변경
    private static final Long TEMP_USER_ID = 1L;

    @Transactional
    public PetResponse create(PetRequest request) {
        Pet pet = Pet.builder()
                .userId(TEMP_USER_ID)
                .name(request.name())
                .breed(request.breed())
                .weight(request.weight())
                .size(request.size())
                .hasCarrier(Boolean.TRUE.equals(request.hasCarrier()))
                .hasStroller(Boolean.TRUE.equals(request.hasStroller()))
                .build();
        return PetResponse.from(petRepository.save(pet));
    }

    public List<PetResponse> findAllByUser() {
        return petRepository.findAllByUserId(TEMP_USER_ID).stream()
                .map(PetResponse::from)
                .toList();
    }

    @Transactional
    public PetResponse update(Long id, PetRequest request) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다: " + id));
        pet.update(request.name(), request.breed(), request.weight(), request.size(),
                request.hasCarrier(), request.hasStroller());
        return PetResponse.from(pet);
    }

    @Transactional
    public void delete(Long id) {
        petRepository.deleteById(id);
    }
}
