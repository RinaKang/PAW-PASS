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
        String issuedDate
) {
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
                common.modifiedTime()
        );
    }

    private static String joinAddr(String addr1, String addr2) {
        if (addr2 == null || addr2.isBlank()) {
            return addr1;
        }
        return addr1 + " " + addr2;
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

    public record PetCondition(
            String acmpyTypeCd,
            String acmpyPsblCpam,
            String acmpyNeedMtr,
            String etcAcmpyInfo
    ) {
        public static PetCondition from(TourDetailPetTourItem pet) {
            if (pet == null) {
                return new PetCondition(null, null, null, null);
            }
            return new PetCondition(pet.acmpyTypeCd(), pet.acmpyPsblCpam(), pet.acmpyNeedMtr(), pet.etcAcmpyInfo());
        }
    }
}
