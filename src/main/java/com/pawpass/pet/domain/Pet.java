package com.pawpass.pet.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    // 프론트 폼에 입력란은 있었지만 대응 컬럼이 없어서 값이 저장되지 않던 필드.
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PetSize size; // SMALL, MEDIUM, LARGE

    @Column(name = "has_carrier", nullable = false)
    private boolean hasCarrier; // 이동장/케이지

    @Column(name = "has_leash", nullable = false)
    private boolean hasLeash; // 목줄/하네스

    // 동반 시 구비 가능한 용품(2026-09-13 추가, 프론트 반려동물 등록 폼 다중선택 항목) - 나머지 4종.
    @Column(name = "has_muzzle", nullable = false)
    private boolean hasMuzzle; // 입마개

    @Column(name = "has_waste_bags", nullable = false)
    private boolean hasWasteBags; // 배변봉투

    @Column(name = "has_stroller", nullable = false)
    private boolean hasStroller; // 유모차/웨건

    @Column(name = "has_diaper", nullable = false)
    private boolean hasDiaper; // 기저귀/매너벨트

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 2026-09-17 추가 - 사용자 프로필 이미지(User.picture)와 같은 방식(서버 로컬 디스크, ProfileImageStorage).
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder
    public Pet(Long userId, String name, String species, String breed, Double weight, LocalDate birthDate,
               PetSize size, boolean hasCarrier, boolean hasLeash, boolean hasMuzzle, boolean hasWasteBags,
               boolean hasStroller, boolean hasDiaper) {
        this.userId = userId;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.weight = weight;
        this.birthDate = birthDate;
        this.size = size;
        this.hasCarrier = hasCarrier;
        this.hasLeash = hasLeash;
        this.hasMuzzle = hasMuzzle;
        this.hasWasteBags = hasWasteBags;
        this.hasStroller = hasStroller;
        this.hasDiaper = hasDiaper;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, String species, String breed, Double weight, LocalDate birthDate, PetSize size,
                        Boolean hasCarrier, Boolean hasLeash, Boolean hasMuzzle, Boolean hasWasteBags,
                        Boolean hasStroller, Boolean hasDiaper) {
        if (name != null) this.name = name;
        if (species != null) this.species = species;
        if (breed != null) this.breed = breed;
        if (weight != null) this.weight = weight;
        if (birthDate != null) this.birthDate = birthDate;
        if (size != null) this.size = size;
        if (hasCarrier != null) this.hasCarrier = hasCarrier;
        if (hasLeash != null) this.hasLeash = hasLeash;
        if (hasMuzzle != null) this.hasMuzzle = hasMuzzle;
        if (hasWasteBags != null) this.hasWasteBags = hasWasteBags;
        if (hasStroller != null) this.hasStroller = hasStroller;
        if (hasDiaper != null) this.hasDiaper = hasDiaper;
    }

    public void updateImage(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
