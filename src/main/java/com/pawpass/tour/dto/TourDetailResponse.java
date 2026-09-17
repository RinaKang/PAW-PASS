package com.pawpass.tour.dto;

import com.pawpass.tour.dto.external.TourDetailCommonItem;
import com.pawpass.tour.dto.external.TourDetailImageItem;
import com.pawpass.tour.dto.external.TourDetailIntroItem;
import com.pawpass.tour.dto.external.TourDetailPetTourItem;

import java.util.List;
import java.util.stream.Stream;

public record TourDetailResponse(
        String contentId,
        String title,
        String addr,
        String tel,
        String hours,
        List<String> images,
        PetCondition petCondition,
        String issuedDate,
        Double mapX,
        Double mapY
) {
    /**
     * mapX/mapY(2026-09-13 추가) - TourAPI 좌표 표기 관례: mapX=경도(longitude), mapY=위도(latitude),
     * TourSummaryResponse(목록)와 동일. 목록엔 있는데 상세엔 없어서 상세 페이지 지도에 핀을 못 찍던
     * 실제 프론트 리포트로 발견됨 - detailCommon2 응답에 mapx/mapy가 이미 있는데 안 받아쓰고 있었음.
     */
    public static TourDetailResponse of(TourDetailCommonItem common, TourDetailIntroItem intro,
                                         TourDetailPetTourItem pet, List<TourDetailImageItem> images) {
        return new TourDetailResponse(
                common.contentId(),
                common.title(),
                joinAddr(common.addr1(), common.addr2()),
                common.tel(),
                intro == null ? null : intro.hours(),
                images(common.firstimage(), common.firstimage2(), images),
                PetCondition.from(pet),
                common.modifiedTime(),
                parseDouble(common.mapx()),
                parseDouble(common.mapy())
        );
    }

    private static String joinAddr(String addr1, String addr2) {
        if (addr2 == null || addr2.isBlank()) {
            return addr1;
        }
        return addr1 + " " + addr2;
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * detailImage2(썸네일 갤러리)를 우선 쓰고, 그 관광지에 detailImage2 등록분이 없으면
     * detailCommon2의 firstimage/firstimage2로 폴백한다 - 상세 화면에 이미지가 아예 안 뜨는 것보단 낫다.
     */
    private static List<String> images(String firstimage, String firstimage2, List<TourDetailImageItem> detailImages) {
        List<String> thumbnails = detailImages == null ? List.of() : detailImages.stream()
                .map(TourDetailImageItem::smallimageurl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
        if (!thumbnails.isEmpty()) {
            return thumbnails;
        }
        return Stream.of(firstimage, firstimage2)
                .filter(url -> url != null && !url.isBlank())
                .toList();
    }

    /**
     * relaPosesFclty/relaFrnshPrdlst/relaPurcPrdlst/relaRntlPrdlst(2026-09-15 추가) - TourAPI
     * detailPetTour2 응답엔 원래도 있던 필드인데 TourDetailPetTourItem에 선언이 안 돼 있어서
     * @JsonIgnoreProperties(ignoreUnknown=true)에 의해 파싱 단계에서 조용히 버려지고 있었다
     * (프론트에서 "이 필드들이 안 나온다"는 리포트로 발견 - 응답 자체엔 있는데 우리 DTO가 안 받았음).
     */
    public record PetCondition(
            String acmpyTypeCd,
            String acmpyPsblCpam,
            String acmpyNeedMtr,
            String etcAcmpyInfo,
            String relaPosesFclty,
            String relaFrnshPrdlst,
            String relaPurcPrdlst,
            String relaRntlPrdlst
    ) {
        public static PetCondition from(TourDetailPetTourItem pet) {
            if (pet == null) {
                return new PetCondition(null, null, null, null, null, null, null, null);
            }
            return new PetCondition(pet.acmpyTypeCd(), pet.acmpyPsblCpam(), pet.acmpyNeedMtr(), pet.etcAcmpyInfo(),
                    pet.relaPosesFclty(), pet.relaFrnshPrdlst(), pet.relaPurcPrdlst(), pet.relaRntlPrdlst());
        }
    }
}
