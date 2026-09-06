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
                .build();
        return PetResponse.from(petRepository.save(pet));
    }

    public List<PetResponse> findAllByUser(Long userId) {
        return petRepository.findAllByUserId(userId).stream()
                .map(PetResponse::from)
                .toList();
    }

    @Transactional
    public PetResponse update(Long userId, Long id, PetRequest request) {
        Pet pet = petRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다: " + id));
        pet.update(request.name(), request.species(), request.breed(), request.weight(), request.size(),
                request.hasCarrier(), request.hasLeash());
        return PetResponse.from(pet);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Pet pet = petRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 반려동물입니다: " + id));
        petRepository.delete(pet);
    }
}
