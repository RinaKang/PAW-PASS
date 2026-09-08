package com.pawpass.facility.service;

import com.pawpass.facility.domain.PetFacility;
import com.pawpass.facility.dto.FacilityDetailResponse;
import com.pawpass.facility.dto.FacilitySummaryResponse;
import com.pawpass.facility.repository.PetFacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private static final int PAGE_SIZE = 20;

    private final PetFacilityRepository petFacilityRepository;

    public List<FacilitySummaryResponse> search(String regionCode, String category, int page) {
        var pageable = PageRequest.of(Math.max(page - 1, 0), PAGE_SIZE);
        return petFacilityRepository.search(regionCode, category, pageable).stream()
                .map(FacilitySummaryResponse::from)
                .toList();
    }

    public FacilityDetailResponse getDetail(String id) {
        PetFacility facility = petFacilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다: " + id));
        return FacilityDetailResponse.from(facility);
    }
}
