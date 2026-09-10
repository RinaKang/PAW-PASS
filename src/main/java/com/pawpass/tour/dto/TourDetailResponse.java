package com.pawpass.tour.dto;

import com.pawpass.tour.dto.external.TourDetailCommonItem;
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
    public static TourDetailResponse of(TourDetailCommonItem common, TourDetailIntroItem intro, TourDetailPetTourItem pet) {
        return new TourDetailResponse(
                common.contentId(),
                common.title(),
                joinAddr(common.addr1(), common.addr2()),
                common.tel(),
                intro == null ? null : intro.hours(),
                images(common.firstimage(), common.firstimage2()),
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

    private static List<String> images(String firstimage, String firstimage2) {
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
