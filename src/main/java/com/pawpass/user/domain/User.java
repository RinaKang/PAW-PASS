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
}
