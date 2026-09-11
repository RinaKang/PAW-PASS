package com.pawpass.facility.service;

import com.pawpass.facility.client.GooglePlacesClient;
import com.pawpass.facility.domain.PetFacility;
import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.dto.FacilitySummaryResponse;
import com.pawpass.facility.repository.PetFacilityRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * 실제로 사진을 불러오고 나머지는 image=null로 내려서 프론트가 플레이스홀더로 처리하게 한다.
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
        String image = googlePlacesClient.fetchPhotoUrl(facility.getGooglePlaceId(), PHOTO_MAX_WIDTH_PX);
        return FacilitySummaryResponse.from(facility, image);
    }

    public FacilityDetailResponse getDetail(String id) {
        PetFacility facility = petFacilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다: " + id));
        return FacilityDetailResponse.from(facility);
    }
}
