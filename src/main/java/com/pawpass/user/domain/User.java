package com.pawpass.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ERD 최종본 기준.
 * 소셜 로그인은 구글만 지원 - 구글 계정 고유 식별자(sub)를 google_id로 저장.
 * UNIQUE(google_id) 제약은 DB 마이그레이션 스크립트에서 설정.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"google_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", nullable = false, length = 100)
    private String googleId;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 500)
    private String picture;

    // 여행 조건 (RQ-01. 1-2) - GPS 미사용, 시군구 단위 선택
    @Column(name = "region_code", length = 20)
    private String regionCode;

    @Column(name = "travel_category", length = 30)
    private String travelCategory;

    // 대표 반려동물(2026-09-13 추가) - 반려동물이 1마리뿐이면 자동으로 이 값이 채워지고(PetService.create),
    // 2마리 이상이면 사용자가 명시적으로 골라야 한다(PUT /users/me/primary-pet). /explore·매칭에서 petId를
    // 안 넘기면 이 값을 기본으로 쓴다 - MatchingService.resolveOptionalPet/requirePetForMatch 참고.
    @Column(name = "primary_pet_id")
    private Long primaryPetId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public User(String googleId, String email, String name, String picture) {
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.picture = picture;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTravelCondition(String regionCode, String travelCategory) {
        this.regionCode = regionCode;
        this.travelCategory = travelCategory;
        this.updatedAt = LocalDateTime.now(); // @PreUpdate는 flush 시점에만 반영되므로, 같은 트랜잭션 내 즉시 응답용으로 직접 설정
    }

    public void changePrimaryPet(Long petId) {
        this.primaryPetId = petId;
        this.updatedAt = LocalDateTime.now();
    }

    /** 직접 업로드한 프로필 이미지로 picture를 덮어쓴다(2026-09-15 추가) - 이전엔 구글 로그인 시점에만 채워졌음. */
    public void updatePicture(String picture) {
        this.picture = picture;
        this.updatedAt = LocalDateTime.now();
    }
}
