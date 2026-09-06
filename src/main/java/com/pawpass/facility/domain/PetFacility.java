package com.pawpass.facility.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 한국문화정보원 API(API_TOU_050) 데이터를 배치로 미러링하는 테이블.
 *
 * ⚠️ 전제조건: 이 테이블은 "캐싱 허용"을 전제로 설계됨.
 * 한국문화정보원 측에 캐싱 가능 여부 문의 답변이 "불허"로 오면
 * 이 엔티티와 facility/batch 패키지는 통째로 제거하고,
 * facility 조회를 tour 패키지처럼 실시간 호출 방식으로 재설계해야 함.
 * (단, 그 경우 위치기반/거리순 검색은 구현 불가해짐 - 이전 논의 참고)
 */
@Entity
@Table(name = "pet_facilities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetFacility {

    @Id
    @Column(length = 64)
    private String id; // title + address 조합 해시 (원본 API에 고유 ID가 없어 자체 생성)

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 30)
    private String category1;

    @Column(length = 30)
    private String category2;

    @Column(length = 300)
    private String address;

    private Double lat;
    private Double lng;

    @Column(length = 30)
    private String tel;

    @Column(length = 500)
    private String url;

    @Column(length = 200)
    private String charge;

    // --- description 원문을 파싱해서 구조화한 필드들 ---
    @Column(name = "operating_hours", length = 200)
    private String operatingHours;

    @Column(name = "closed_days", length = 100)
    private String closedDays;

    @Column(name = "parking_available")
    private Boolean parkingAvailable;

    @Column(name = "pet_allowed")
    private Boolean petAllowed;

    @Column(name = "pet_restriction", length = 300)
    private String petRestriction;

    @Column(name = "description_raw", length = 65535)
    private String descriptionRaw; // 파싱 실패 대비 원문 그대로 보관 (length=65535 -> Hibernate가 MySQL TEXT로 매핑)

    @Column(name = "issued_date")
    private String issuedDate; // 원본 API의 issuedDate 

    @Setter
    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Builder
    public PetFacility(String id, String title, String category1, String category2,
                        String address, Double lat, Double lng, String tel, String url, String charge,
                        String operatingHours, String closedDays, Boolean parkingAvailable,
                        Boolean petAllowed, String petRestriction, String descriptionRaw, String issuedDate) {
        this.id = id;
        this.title = title;
        this.category1 = category1;
        this.category2 = category2;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.tel = tel;
        this.url = url;
        this.charge = charge;
        this.operatingHours = operatingHours;
        this.closedDays = closedDays;
        this.parkingAvailable = parkingAvailable;
        this.petAllowed = petAllowed;
        this.petRestriction = petRestriction;
        this.descriptionRaw = descriptionRaw;
        this.issuedDate = issuedDate;
        this.syncedAt = LocalDateTime.now();
    }
}
