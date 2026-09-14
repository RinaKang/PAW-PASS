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
    // syncedAt 단독 정렬은 같은 배치로 동기화된 행끼리 값이 동일해서 LIMIT/OFFSET 페이징 시 동률 행 순서가
    // 호출마다 달라질 수 있다(2026-09-14, 같은 조건으로 반복 호출했을 때 목록 구성 자체가 흔들리는 걸 실측함) -
    // id를 2차 정렬키로 둬서 동률을 항상 같은 순서로 깬다.
    @Query("""
            SELECT f FROM PetFacility f
            WHERE (:region IS NULL OR f.address LIKE CONCAT('%', :region, '%'))
              AND (:category IS NULL OR f.category1 = :category OR f.category2 = :category OR f.category3 = :category)
            ORDER BY f.syncedAt DESC, f.id ASC
            """)
    List<PetFacility> search(@Param("region") String region, @Param("category") String category, Pageable pageable);

    /**
     * /explore의 keyword 검색(동선 화면 장소 검색 등)용 - 지역/카테고리 구분 없이 이름 또는 주소로 찾는다
     * ("위치나 시설 이름 등"으로 검색하고 싶다는 요청이라 둘 다 대상으로 잡음, 2026-09-14).
     */
    @Query("""
            SELECT f FROM PetFacility f
            WHERE f.title LIKE CONCAT('%', :keyword, '%') OR f.address LIKE CONCAT('%', :keyword, '%')
            ORDER BY f.syncedAt DESC, f.id ASC
            """)
    List<PetFacility> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
