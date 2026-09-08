package com.pawpass.facility.repository;

import com.pawpass.facility.domain.PetFacility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PetFacilityRepository extends JpaRepository<PetFacility, String> {

    /**
     * region은 법정동 코드가 아니라 address 문자열에 대한 부분 일치 검색이다.
     * KCISA 원본 데이터엔 지역 "코드"가 없고 시도/시군구 "명칭"만 있어서(반면 User.regionCode/TourAPI는 코드 체계),
     * 지금은 address LIKE 매칭으로 임시 대응 - explore 도메인에서 코드↔명칭 매핑을 붙이면 개선 필요.
     */
    @Query("""
            SELECT f FROM PetFacility f
            WHERE (:region IS NULL OR f.address LIKE CONCAT('%', :region, '%'))
              AND (:category IS NULL OR f.category1 = :category OR f.category2 = :category OR f.category3 = :category)
            ORDER BY f.syncedAt DESC
            """)
    List<PetFacility> search(@Param("region") String region, @Param("category") String category, Pageable pageable);
}
