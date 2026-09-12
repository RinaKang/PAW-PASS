package com.pawpass.facility.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 한국문화정보원_전국 반려동물 동반 가능 문화시설 위치 데이터(공공데이터포털, odcloud.kr 게이트웨이)를
 * 배치로 미러링하는 테이블. 원본 API는 반려동물 동반 가능(Y)/불가(N) 시설을 모두 포함하므로,
 * 동기화 시 pet_allowed=true(원문 "Y")인 것만 저장한다.
 *
 * ⚠️ 전제조건: 이 테이블은 "캐싱 허용"을 전제로 설계됨 (2026-09 문의 결과 "모두에게 열려있고 사용 가능"으로 확인됨).
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

    @Column(length = 30)
    private String category3;

    @Column(length = 300)
    private String address;

    @Column(length = 10)
    private String zipcode;

    private Double lat;
    private Double lng;

    @Column(length = 30)
    private String tel;

    @Column(length = 500)
    private String url;

    @Column(length = 200)
    private String charge;

    @Column(name = "operating_hours", length = 200)
    private String operatingHours;

    @Column(name = "closed_days", length = 100)
    private String closedDays;

    @Column(name = "parking_available")
    private Boolean parkingAvailable;

    @Column(name = "pet_allowed")
    private Boolean petAllowed;

    @Column(name = "pet_exclusive", length = 50)
    private String petExclusive; // 반려동물 전용 정보 (원문이 Y/N이 아니라 "해당없음" 등 서술형이라 String으로 보관)

    @Column(name = "allowed_pet_size", length = 65535)
    private String allowedPetSize; // 입장 가능 동물 크기. 실측상 단순 "모두 가능"이 아니라 괄호+쉼표 나열형 서술도 있어 length=65535 -> TEXT로 매핑

    @Column(name = "pet_restriction", length = 300)
    private String petRestriction;

    @Column(name = "indoor")
    private Boolean indoor;

    @Column(name = "outdoor")
    private Boolean outdoor;

    @Column(name = "additional_pet_fee", length = 100)
    private String additionalPetFee;

    @Column(name = "description_raw", length = 65535)
    private String descriptionRaw; // 기본 정보_장소설명. 파싱 실패 대비 원문 그대로 보관 (length=65535 -> Hibernate가 MySQL TEXT로 매핑)

    @Column(name = "issued_date", length = 20)
    private String issuedDate; // 원본 API의 최종작성일

    @Setter
    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    // Places API(New) place_id - 배치가 아니라 FacilityService가 이 시설이 실제로 조회될 때 지연 해소해서 채운다.
    // 사진 이름/URL과 달리 이 값은 캐싱 가능해서 여기만 저장하고, 사진 자체는 매번 재조회한다.
    // null=아직 시도 안 함, ""=시도했는데 못 찾음(재시도 안 함), 그 외=실제 place_id.
    @Setter
    @Column(name = "google_place_id", length = 255)
    private String googlePlaceId;

    @Builder
    public PetFacility(String id, String title, String category1, String category2, String category3,
                        String address, String zipcode, Double lat, Double lng, String tel, String url, String charge,
                        String operatingHours, String closedDays, Boolean parkingAvailable,
                        Boolean petAllowed, String petExclusive, String allowedPetSize, String petRestriction,
                        Boolean indoor, Boolean outdoor, String additionalPetFee,
                        String descriptionRaw, String issuedDate) {
        this.id = id;
        this.title = title;
        this.category1 = category1;
        this.category2 = category2;
        this.category3 = category3;
        this.address = address;
        this.zipcode = zipcode;
        this.lat = lat;
        this.lng = lng;
        this.tel = tel;
        this.url = url;
        this.charge = charge;
        this.operatingHours = operatingHours;
        this.closedDays = closedDays;
        this.parkingAvailable = parkingAvailable;
        this.petAllowed = petAllowed;
        this.petExclusive = petExclusive;
        this.allowedPetSize = allowedPetSize;
        this.petRestriction = petRestriction;
        this.indoor = indoor;
        this.outdoor = outdoor;
        this.additionalPetFee = additionalPetFee;
        this.descriptionRaw = descriptionRaw;
        this.issuedDate = issuedDate;
        this.syncedAt = LocalDateTime.now();
    }
}
