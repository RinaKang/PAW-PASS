package com.pawpass.favorite.domain;

import com.pawpass.global.util.DataSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "favorites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "source", "content_id"})
        // 동일 사용자가 같은 장소를 중복 즐겨찾기하지 못하도록 하는 제약 (RQ-07 비즈니스 규칙)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSource source;

    // TOURAPI면 관광공사 contentid, KCISA면 pet_facilities.id를 문자열로 저장
    @Column(name = "content_id", nullable = false, length = 100)
    private String contentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Favorite(Long userId, DataSource source, String contentId) {
        this.userId = userId;
        this.source = source;
        this.contentId = contentId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
