package com.pawpass.facility.service;

import com.pawpass.facility.client.GooglePlacesClient;
import com.pawpass.facility.domain.PetFacility;
import com.pawpass.facility.dto.FacilitySummaryResponse;
import com.pawpass.facility.repository.PetFacilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityServiceTest {

    @Mock
    private PetFacilityRepository petFacilityRepository;

    @Mock
    private GooglePlacesClient googlePlacesClient;

    private FacilityService facilityService;

    private void init() {
        facilityService = new FacilityService(petFacilityRepository, googlePlacesClient);
    }

    @Test
    void place_id가_있는_시설은_구글에서_받은_사진_url을_그대로_실어준다() {
        init();
        PetFacility facility = facility("f1");
        facility.setGooglePlaceId("places/abc");
        when(petFacilityRepository.search(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(facility));
        when(googlePlacesClient.fetchPhotoUrl("places/abc", 800)).thenReturn("https://lh3.googleusercontent.com/photo");

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        assertThat(result).extracting(FacilitySummaryResponse::image)
                .containsExactly("https://lh3.googleusercontent.com/photo");
    }

    @Test
    void place_id가_없는_시설은_구글_호출_없이_image가_null이다() {
        init();
        PetFacility facility = facility("f1"); // googlePlaceId 미설정

        when(petFacilityRepository.search(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(facility));

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        assertThat(result).extracting(FacilitySummaryResponse::image).containsExactly(new String[]{null});
        verify(googlePlacesClient).fetchPhotoUrl(null, 800); // null 전달 시 클라이언트 내부에서 조기 반환
    }

    @Test
    void 한_페이지에서_사진_조회는_상한개수까지만_시도하고_나머지는_null이다() {
        init();
        List<PetFacility> facilities = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            PetFacility facility = facility("f" + i);
            facility.setGooglePlaceId("places/" + i);
            facilities.add(facility);
        }
        when(petFacilityRepository.search(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(facilities);
        when(googlePlacesClient.fetchPhotoUrl(anyString(), anyInt())).thenReturn("https://img");

        List<FacilitySummaryResponse> result = facilityService.search(null, null, 1);

        long withImage = result.stream().filter(r -> r.image() != null).count();
        assertThat(withImage).isEqualTo(10); // MAX_ITEMS_TO_FETCH_IMAGE
        verify(googlePlacesClient, times(10)).fetchPhotoUrl(anyString(), anyInt());
    }

    private PetFacility facility(String id) {
        return PetFacility.builder()
                .id(id).title("시설-" + id).address("주소").issuedDate("2026-01-01")
                .build();
    }
}
