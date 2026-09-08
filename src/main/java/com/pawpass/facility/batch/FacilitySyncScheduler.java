package com.pawpass.facility.batch;

import com.pawpass.facility.service.FacilitySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 한국문화정보원 데이터를 1일 1회 배치로 호출해 pet_facilities 테이블에 upsert.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacilitySyncScheduler {

    private final FacilitySyncService facilitySyncService;

    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
    public void syncFacilities() {
        log.info("한국문화정보원 데이터 동기화 시작");
        facilitySyncService.syncAll();
        log.info("한국문화정보원 데이터 동기화 완료");
    }
}
