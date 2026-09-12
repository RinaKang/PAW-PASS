package com.pawpass.facility.service;

import com.pawpass.facility.client.GooglePlacesClient;
import com.pawpass.facility.domain.PetFacility;
import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.dto.FacilityImageResponse;
import com.pawpass.facility.dto.FacilitySummaryResponse;
import com.pawpass.facility.repository.PetFacilityRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private static final int PAGE_SIZE = 20;

    /**
     * 구글 Places 사진 URL은 캐싱 금지라 목록 조회 때마다 실시간으로 다시 받아야 하는데, 페이지(20건) 전부에
     * 대해 매번 place details + photo media(최대 2회씩) 호출하면 /explore 매칭에서 이미 한 번 겪은
     * "항목 수만큼 외부 호출이 곱해져서 응답이 느려지는" 문제가 사진 쪽에서 그대로 재현된다.
     * tour 매칭 상한(ExploreService.MAX_TOUR_ITEMS_TO_MATCH)과 같은 취지로, 페이지당 이 개수까지만
     * 실제로 사진을 불러오고(+ 아래 place_id 지연 해소도 이 개수까지만) 나머지는 image=null로 내려서
     * 프론트가 플레이스홀더로 처리하게 한다.
     *
     * 이 값이 이제 구글 API 비용 상한 역할도 겸한다(2026-09-12, place_id를 배치로 전량 미리 채워두는 대신
     * 조회될 때 그때 찾는 방식으로 바꾸면서) - 21,000건 전체를 미리 채워둘 필요 없이, 실제로 화면에
     * 노출되는 시설만 그때그때 찾아서 캐싱하므로 사용량 자체가 실제 트래픽에 비례해 자연히 작게 유지된다.
     */
    private static final int MAX_ITEMS_TO_FETCH_IMAGE = 10;
    private static final int PHOTO_MAX_WIDTH_PX = 800;
    private static final int IMAGE_FETCH_CONCURRENCY = 8;

    private final PetFacilityRepository petFacilityRepository;
    private final GooglePlacesClient googlePlacesClient;

    // ExploreService.matchExecutor와 동일한 이유로 데몬 스레드 사용
    private final ExecutorService imageFetchExecutor = Executors.newFixedThreadPool(IMAGE_FETCH_CONCURRENCY, runnable -> {
        Thread thread = new Thread(runnable, "facility-image");
        thread.setDaemon(true);
        return thread;
    });

    @PreDestroy
    void shutdown() {
        imageFetchExecutor.shutdown();
    }

    public List<FacilitySummaryResponse> search(String regionCode, String category, int page) {
        var pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE);
        List<PetFacility> facilities = petFacilityRepository.search(regionCode, category, pageable);

        List<CompletableFuture<FacilitySummaryResponse>> futures = new ArrayList<>();
        int remainingImageBudget = MAX_ITEMS_TO_FETCH_IMAGE;
        for (PetFacility facility : facilities) {
            boolean withinBudget = remainingImageBudget-- > 0;
            futures.add(withinBudget
                    ? CompletableFuture.supplyAsync(() -> toSummaryWithImage(facility), imageFetchExecutor)
                    : CompletableFuture.completedFuture(FacilitySummaryResponse.from(facility)));
        }
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private FacilitySummaryResponse toSummaryWithImage(PetFacility facility) {
        FacilityImageResponse image = resolveImage(facility);
        return FacilitySummaryResponse.from(facility, image.image(), image.imageAttribution());
    }

    /**
     * 시설 하나의 사진만 가져온다 - GET /facilities/{id}/image 전용. 시설 상세(getDetail())는 구글 호출을
     * 전혀 하지 않고 즉시 응답하고, 프론트가 이 메서드(엔드포인트)를 병렬/지연 호출로 따로 불러서 사진만
     * 나중에 채워 넣게 한다(FacilityImageResponse 주석 참고) - place_id 캐시는 되지만 사진 URL 자체는
     * 매번 실시간 조회라 이 호출 자체의 지연은 없앨 수 없고, 단지 시설 본문 응답 경로에서 분리할 뿐이다.
     */
    public FacilityImageResponse getImage(String id) {
        PetFacility facility = petFacilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다: " + id));
        return resolveImage(facility);
    }

    private FacilityImageResponse resolveImage(PetFacility facility) {
        String placeId = resolvePlaceId(facility);
        if (placeId == null || placeId.isBlank()) {
            return FacilityImageResponse.EMPTY;
        }
        GooglePlacesClient.PlacePhoto photo = googlePlacesClient.fetchPhoto(placeId, PHOTO_MAX_WIDTH_PX);
        return photo == null ? FacilityImageResponse.EMPTY : new FacilityImageResponse(photo.url(), photo.attribution());
    }

    /**
     * 이 시설이 실제로 목록에 노출되는 시점에 place_id를 지연 해소한다 - 21,000여 건 전체를 배치로 미리
     * 돌 필요 없이, 실제로 조회되는 시설만 그때그때 찾아서 DB에 캐싱한다(다음부턴 재사용).
     * 정상 조회했는데 못 찾은 경우엔 ""을 저장해 "찾아봤지만 없음"으로 확정 캐싱한다(재시도 안 함) -
     * 반면 호출 자체가 실패한 경우(레이트리밋/네트워크 등)는 저장하지 않고 null 그대로 둬서 다음 조회 때
     * 다시 시도할 수 있게 한다(GooglePlacesClient.searchPlaceId 주석 참고).
     * 같은 시설이 동시에 여러 요청으로 처음 조회되면 중복으로 검색될 수 있는데(레이스), 그 정도 낭비는
     * 감수할 만한 수준이라 별도 락은 안 둔다.
     */
    private String resolvePlaceId(PetFacility facility) {
        if (facility.getGooglePlaceId() != null) {
            return facility.getGooglePlaceId();
        }
        try {
            String resolved = googlePlacesClient.searchPlaceId(facility.getTitle() + " " + facility.getAddress());
            facility.setGooglePlaceId(resolved == null ? "" : resolved);
            petFacilityRepository.save(facility);
            return facility.getGooglePlaceId();
        } catch (Exception e) {
            log.warn("시설 place_id 조회 실패 - 다음 조회 때 재시도함: id={}, error={}", facility.getId(), e.getMessage());
            return null;
        }
    }

    public FacilityDetailResponse getDetail(String id) {
        PetFacility facility = petFacilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다: " + id));
        return FacilityDetailResponse.from(facility);
    }
}
