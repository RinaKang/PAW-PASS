package com.pawpass.facility.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 한국문화정보원 API를 1일 1회 배치로 호출해 pet_facilities 테이블에 upsert.
 * 우선 category=여행지만 수집 (박물관/문예회관/펜션/호텔 등은 필요시 확장).
 *
 * TODO: KcisaApiClient 구현 (전체 페이지네이션 순회하며 수집)
 * TODO: FacilityParser로 description "|" split 파싱 (운영시간/휴무일/주차/동반가능여부/제한사항)
 * TODO: title+address 해시로 PK 생성 후 upsert
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FacilitySyncScheduler {

    // private final KcisaApiClient kcisaApiClient;
    // private final FacilityParser facilityParser;
    // private final FacilityRepository facilityRepository;

    @Scheduled(cron = "0 0 4 * * *") // 매일 새벽 4시
    public void syncFacilities() {
        log.info("한국문화정보원 데이터 동기화 시작");
        // TODO: 구현
        log.info("한국문화정보원 데이터 동기화 완료");
    }
}
