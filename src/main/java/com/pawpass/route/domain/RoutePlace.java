package com.pawpass.route.domain;

import com.pawpass.global.util.DataSource;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자가 편집 중인 동선 후보 장소 하나 - 사용자당 1개의 목록을 이룬다(favorite/trip과 달리 순서가 있음).
 * title/lat/lng는 프론트가 저장 시점에 보낸 값을 그대로 저장한다 - Favorite/Trip처럼 PlaceLookupService로
 * 매번 재조회하지 않는다(목록을 열 때마다 TourAPI/KCISA를 다시 부르면 이미 빠듯한 TourAPI 일일 호출
 * 한도를 또 깎아먹기 때문, 2026-09-18 TourAPI 호출량 절감 작업과 같은 이유).
 */
@Entity
@Table(name = "route_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoutePlace {

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

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    // 배열 순서 = 동선 순서. 전체 교체(SavedRouteService.replaceAll)할 때마다 0부터 다시 매긴다.
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RoutePlace(Long userId, DataSource source, String contentId, String title,
                       Double lat, Double lng, Integer sortOrder) {
        this.userId = userId;
        this.source = source;
        this.contentId = contentId;
        this.title = title;
        this.lat = lat;
        this.lng = lng;
        this.sortOrder = sortOrder;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
