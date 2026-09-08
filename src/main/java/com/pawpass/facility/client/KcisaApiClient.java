package com.pawpass.facility.client;

import com.pawpass.facility.dto.external.KcisaFacilityListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 한국문화정보원_전국 반려동물 동반 가능 문화시설 위치 데이터 (공공데이터포털, odcloud.kr 게이트웨이) 호출 클라이언트.
 * TourAPI와 달리 지역/카테고리 등 서버단 필터 파라미터가 전혀 없음 - page/perPage로 전체(약 70,650건)를 순회해야 함.
 * 배치(facility/service/FacilitySyncService)에서만 호출하고, 이 클라이언트 자체는 캐싱하지 않음.
 */
@Component
@RequiredArgsConstructor
public class KcisaApiClient {

    private final WebClient kcisaApiWebClient;

    @Value("${external-api.kcisa-api.service-key}")
    private String serviceKey;

    public KcisaFacilityListResponse fetchPage(int page, int perPage) {
        return kcisaApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("page", page)
                        .queryParam("perPage", perPage)
                        .queryParam("returnType", "JSON")
                        .queryParam("serviceKey", decodedServiceKey())
                        .build())
                .retrieve()
                .bodyToMono(KcisaFacilityListResponse.class)
                .block();
    }

    /** data.go.kr 계정 공용 인증키는 이미 URL-Encode된 형태로 발급됨 - 이중 인코딩 방지용 디코딩 (TourApiClient와 동일 이유) */
    private String decodedServiceKey() {
        return URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);
    }
}
