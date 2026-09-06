package com.pawpass.pet.repository;

import com.pawpass.pet.domain.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findAllByUserId(Long userId);
    Optional<Pet> findByIdAndUserId(Long id, Long userId);
    void deleteAllByUserId(Long userId);
}
