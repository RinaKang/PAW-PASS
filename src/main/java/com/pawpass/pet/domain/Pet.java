package com.pawpass.pet.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 연관관계 매핑 대신 ID만 저장 - 팀 컨벤션에 따라 @ManyToOne으로 바꿔도 됨

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 30)
    private String species; // 종류 (예: 강아지, 고양이)

    @Column(nullable = false, length = 30)
    private String breed;

    @Column(nullable = false)
    private Double weight; // kg

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PetSize size; // SMALL, MEDIUM, LARGE

    @Column(name = "has_carrier", nullable = false)
    private boolean hasCarrier;

    @Column(name = "has_leash", nullable = false)
    private boolean hasLeash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Pet(Long userId, String name, String species, String breed, Double weight, PetSize size,
               boolean hasCarrier, boolean hasLeash) {
        this.userId = userId;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.weight = weight;
        this.size = size;
        this.hasCarrier = hasCarrier;
        this.hasLeash = hasLeash;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, String species, String breed, Double weight, PetSize size,
                        Boolean hasCarrier, Boolean hasLeash) {
        if (name != null) this.name = name;
        if (species != null) this.species = species;
        if (breed != null) this.breed = breed;
        if (weight != null) this.weight = weight;
        if (size != null) this.size = size;
        if (hasCarrier != null) this.hasCarrier = hasCarrier;
        if (hasLeash != null) this.hasLeash = hasLeash;
    }
}
