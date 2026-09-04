package com.pawpass.trip.domain;

import com.pawpass.global.util.DataSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trips")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DataSource source;

    @Column(name = "content_id", nullable = false, length = 100)
    private String contentId;

    @Column(name = "visited_at", nullable = false)
    private LocalDate visitedAt; // 미입력 시 오늘 날짜로 서비스 레이어에서 기본값 설정 (RQ-07 예외처리)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Trip(Long userId, DataSource source, String contentId, LocalDate visitedAt) {
        this.userId = userId;
        this.source = source;
        this.contentId = contentId;
        this.visitedAt = (visitedAt != null) ? visitedAt : LocalDate.now();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
