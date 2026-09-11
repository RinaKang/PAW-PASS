package com.pawpass.facility.service;

import com.pawpass.facility.client.GooglePlacesClient;
import com.pawpass.facility.client.KcisaApiClient;
import com.pawpass.facility.domain.PetFacility;
import com.pawpass.facility.dto.external.KcisaFacilityItem;
import com.pawpass.facility.dto.external.KcisaFacilityListResponse;
import com.pawpass.facility.repository.PetFacilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilitySyncServiceTest {

    @Mock
    private KcisaApiClient kcisaApiClient;

    @Mock
    private PetFacilityRepository petFacilityRepository;

    @Mock
    private GooglePlacesClient googlePlacesClient;

    private FacilitySyncService facilitySyncService;

    private void init() {
        facilitySyncService = new FacilitySyncService(kcisaApiClient, petFacilityRepository, googlePlacesClient);
    }

    @Test
    void 기존에_place_id가_있으면_새로_검색하지_않고_그대로_이어받는다() {
        init();
        KcisaFacilityItem item = facilityItem("반려카페", "서울시 강남구");
        when(kcisaApiClient.fetchPage(1, 1000))
                .thenReturn(new KcisaFacilityListResponse(1, 1000, 1, 1, 1, List.of(item)));
        when(petFacilityRepository.findAllById(org.mockito.ArgumentMatchers.anySet()))
                .thenAnswer(invocation -> {
                    PetFacility existing = PetFacility.builder()
                            .id(onlyId(invocation.getArgument(0)))
                            .title("반려카페").address("서울시 강남구").issuedDate("2026-01-01")
                            .build();
                    existing.setGooglePlaceId("places/existing-id");
                    return List.of(existing);
                });

        facilitySyncService.syncAll();

        verify(googlePlacesClient, never()).searchPlaceId(anyString());
        ArgumentCaptor<List<PetFacility>> captor = ArgumentCaptor.forClass(List.class);
        verify(petFacilityRepository).saveAll(captor.capture());
        // issuedDate가 같아 데이터 변경은 없지만, place_id가 이미 있었으니 새로 저장할 필요도 없다
        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void place_id가_없으면_예산이_남아있는_한도내에서_새로_검색해서_저장한다() {
        init();
        KcisaFacilityItem item = facilityItem("반려카페", "서울시 강남구");
        when(kcisaApiClient.fetchPage(1, 1000))
                .thenReturn(new KcisaFacilityListResponse(1, 1000, 1, 1, 1, List.of(item)));
        when(petFacilityRepository.findAllById(org.mockito.ArgumentMatchers.anySet()))
                .thenReturn(List.of()); // 기존 데이터 없음(place_id도 당연히 없음)
        when(googlePlacesClient.searchPlaceId("반려카페 서울시 강남구")).thenReturn("places/new-id");

        facilitySyncService.syncAll();

        verify(googlePlacesClient).searchPlaceId("반려카페 서울시 강남구");
        ArgumentCaptor<List<PetFacility>> captor = ArgumentCaptor.forClass(List.class);
        verify(petFacilityRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getGooglePlaceId()).isEqualTo("places/new-id");
    }

    @Test
    void 한_실행에서_신규_place_id_조회_상한을_넘기면_더이상_검색하지_않는다() throws Exception {
        init();
        int overCap = 302; // MAX_NEW_PLACE_ID_RESOLUTIONS_PER_RUN(300)보다 많게
        List<KcisaFacilityItem> items = new java.util.ArrayList<>();
        for (int i = 0; i < overCap; i++) {
            items.add(facilityItem("시설" + i, "주소" + i));
        }
        when(kcisaApiClient.fetchPage(1, 1000))
                .thenReturn(new KcisaFacilityListResponse(1, 1000, overCap, overCap, overCap, items));
        when(petFacilityRepository.findAllById(org.mockito.ArgumentMatchers.anySet()))
                .thenReturn(List.of());
        when(googlePlacesClient.searchPlaceId(anyString())).thenReturn("places/resolved");

        facilitySyncService.syncAll();

        int cap = capValue();
        verify(googlePlacesClient, times(cap)).searchPlaceId(anyString());
    }

    private int capValue() throws Exception {
        Field field = FacilitySyncService.class.getDeclaredField("MAX_NEW_PLACE_ID_RESOLUTIONS_PER_RUN");
        field.setAccessible(true);
        return field.getInt(null);
    }

    private KcisaFacilityItem facilityItem(String title, String roadAddress) {
        return new KcisaFacilityItem(
                title, "카테고리1", "카테고리2", "카테고리3",
                "서울", "강남구", "역삼동", null, "1", roadAddress, "1",
                "37.5", "127.0", null, roadAddress, roadAddress,
                "02-1234-5678", null, null, null,
                "Y", null, "Y", null, null, null,
                "Y", "N", null, null, "2026-01-01"
        );
    }

    /** findAllById(Set<String>)로 넘어온 id 하나를 꺼낸다 (테스트에서 후보의 id를 그대로 기존 엔티티 id로 재사용하기 위함) */
    @SuppressWarnings("unchecked")
    private String onlyId(Object idsArgument) {
        return ((java.util.Set<String>) idsArgument).iterator().next();
    }
}
