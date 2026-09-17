package com.pawpass.facility.service;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 실제 API에 지역/카테고리 필터가 없어 페이지네이션으로 전체(약 70,650건)를 순회해야 하는데, 이 루프가
 * "이 페이지 응답이 비어있으면 끝났다"고 단정하던 게 문제였다(2026-09-19) - 실행마다 21,173건/67,839건으로
 * 총 처리 건수가 들쭉날쭉했던 실제 증상을 조사하다 발견. totalCount 대비 아직 다 순회 못 했는데 빈 응답이
 * 오면 재시도하고, 재시도까지 소진되면 "중단됨"으로 구분해서 로그를 남기도록 고쳤다 - 이 테스트는 그 판단
 * 로직(재시도 성공/소진/정상 종료 구분)과 기존 저장/스킵 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class FacilitySyncServiceTest {

    private static final int PAGE_SIZE = 1000; // FacilitySyncService.PAGE_SIZE와 동일 (private라 여기서도 그대로 씀)

    @Mock
    private KcisaApiClient kcisaApiClient;
    @Mock
    private PetFacilityRepository petFacilityRepository;

    private FacilitySyncService service;

    private void init() {
        service = new FacilitySyncService(kcisaApiClient, petFacilityRepository);
    }

    @Test
    void 반려동물_동반_불가_항목은_저장_대상에서_제외한다() {
        init();
        when(kcisaApiClient.fetchPage(1, PAGE_SIZE)).thenReturn(response(
                10, List.of(item("동반가능곳", "N"), item("동반불가곳", "Y"))));
        when(petFacilityRepository.findAllById(any())).thenReturn(List.of());

        service.syncAll();

        ArgumentCaptor<List<PetFacility>> captor = ArgumentCaptor.forClass(List.class);
        verify(petFacilityRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTitle()).isEqualTo("동반불가곳");
    }

    @Test
    void issuedDate가_그대로면_저장을_건너뛴다() {
        init();
        KcisaFacilityItem sameItem = item("변경없는곳", "Y", "20260101");
        when(kcisaApiClient.fetchPage(1, PAGE_SIZE)).thenReturn(response(10, List.of(sameItem)));
        PetFacility existing = PetFacility.builder()
                .id(hashOf("변경없는곳", "주소")).title("변경없는곳").issuedDate("20260101").build();
        when(petFacilityRepository.findAllById(any())).thenReturn(List.of(existing));

        service.syncAll();

        verify(petFacilityRepository).saveAll(List.of());
    }

    @Test
    void issuedDate가_바뀌었으면_저장하고_기존_googlePlaceId는_이어받는다() {
        init();
        KcisaFacilityItem changedItem = item("바뀐곳", "Y", "20260201");
        when(kcisaApiClient.fetchPage(1, PAGE_SIZE)).thenReturn(response(10, List.of(changedItem)));
        PetFacility existing = PetFacility.builder()
                .id(hashOf("바뀐곳", "주소")).title("바뀐곳").issuedDate("20260101").build();
        existing.setGooglePlaceId("places/already-resolved");
        when(petFacilityRepository.findAllById(any())).thenReturn(List.of(existing));

        service.syncAll();

        ArgumentCaptor<List<PetFacility>> captor = ArgumentCaptor.forClass(List.class);
        verify(petFacilityRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getGooglePlaceId()).isEqualTo("places/already-resolved");
    }

    @Test
    void totalCount만큼_다_순회하면_다음_페이지를_요청하지_않는다() {
        init();
        // totalCount=1000이면 1페이지(1*1000>=1000)로 끝 - 2페이지는 아예 요청 안 해야 한다.
        when(kcisaApiClient.fetchPage(1, PAGE_SIZE)).thenReturn(response(1000, List.of(item("곳1", "Y"))));
        when(petFacilityRepository.findAllById(any())).thenReturn(List.of());

        service.syncAll();

        verify(kcisaApiClient, times(1)).fetchPage(eq(1), eq(PAGE_SIZE));
        verify(kcisaApiClient, never()).fetchPage(eq(2), any(Integer.class));
    }

    @Test
    void 첫_페이지가_정말_비어있으면_중단이_아니라_정상_종료로_처리한다() {
        init();
        // totalCount=0 - 첫 페이지부터 빈 건 "진짜 결과 없음"이지 재시도 대상이 아니다.
        when(kcisaApiClient.fetchPage(1, PAGE_SIZE)).thenReturn(response(0, List.of()));

        service.syncAll();

        verify(kcisaApiClient, times(1)).fetchPage(any(Integer.class), any(Integer.class));
        verify(petFacilityRepository, never()).saveAll(any());
    }

    // 핵심 케이스: totalCount(1500)에 한참 못 미친 2페이지에서 빈 응답을 받으면 "끝났다"고 단정하지 않고
    // 재시도해야 한다 - 재시도 중 정상 데이터가 오면 그대로 이어서 순회를 계속한다.
    @Test
    void 중간_페이지가_예상보다_일찍_비면_재시도해서_성공하면_계속_순회한다() {
        init();
        when(kcisaApiClient.fetchPage(1, PAGE_SIZE)).thenReturn(response(1500, List.of(item("1페이지곳", "Y"))));
        when(kcisaApiClient.fetchPage(2, PAGE_SIZE))
                .thenReturn(response(1500, List.of())) // 1차: 비어있음(글리치)
                .thenReturn(response(1500, List.of(item("2페이지곳", "Y")))); // 재시도: 정상 데이터
        when(petFacilityRepository.findAllById(any())).thenReturn(List.of());

        service.syncAll();

        verify(kcisaApiClient, times(2)).fetchPage(eq(2), eq(PAGE_SIZE));
        ArgumentCaptor<List<PetFacility>> captor = ArgumentCaptor.forClass(List.class);
        verify(petFacilityRepository, times(2)).saveAll(captor.capture());
        assertThat(captor.getAllValues().stream().flatMap(List::stream).map(PetFacility::getTitle))
                .containsExactlyInAnyOrder("1페이지곳", "2페이지곳");
    }

    // 재시도까지 전부 소진되면 그 이상 페이지는 시도하지 않고 중단해야 한다(이미 처리한 건 그대로 저장됨).
    @Test
    void 재시도가_전부_소진되면_더_이상_진행하지_않고_중단한다() {
        init();
        when(kcisaApiClient.fetchPage(1, PAGE_SIZE)).thenReturn(response(3000, List.of(item("1페이지곳", "Y"))));
        when(kcisaApiClient.fetchPage(2, PAGE_SIZE)).thenReturn(response(3000, List.of())); // 매번 비어있음
        when(petFacilityRepository.findAllById(any())).thenReturn(List.of());

        service.syncAll();

        // 최초 1회 + 재시도 3회 = 4회까지만 시도하고 포기
        verify(kcisaApiClient, times(4)).fetchPage(eq(2), eq(PAGE_SIZE));
        verify(kcisaApiClient, never()).fetchPage(eq(3), any(Integer.class));
        // 1페이지에서 이미 처리한 건 그대로 저장돼 있어야 한다 - 중단됐다고 이전 성과까지 버리면 안 됨
        verify(petFacilityRepository, times(1)).saveAll(any());
    }

    private KcisaFacilityListResponse response(int totalCount, List<KcisaFacilityItem> data) {
        return new KcisaFacilityListResponse(1, PAGE_SIZE, totalCount, data.size(), data.size(), data);
    }

    private KcisaFacilityItem item(String title, String petAllowed) {
        return item(title, petAllowed, "20260101");
    }

    private KcisaFacilityItem item(String title, String petAllowed, String issuedDate) {
        return new KcisaFacilityItem(
                title, null, null, null, null, null, null, null, null, null, null,
                null, null, null, "주소", "주소", null, null, null, null, null, null,
                petAllowed, null, null, null, null, null, null, null, issuedDate);
    }

    private String hashOf(String title, String address) {
        // FacilitySyncService.generateId()와 동일한 SHA-256(title|address) 해시 - private라 테스트에서 재현
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((title + "|" + address).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
