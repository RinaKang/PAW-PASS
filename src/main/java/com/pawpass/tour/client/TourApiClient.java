package com.pawpass.tour.client;

import com.pawpass.tour.dto.external.TourAreaBasedListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 한국관광공사 TourAPI 4.0(KorPetTourService2) 실시간 호출 클라이언트.
 * 절대 응답을 DB에 저장하지 말 것 (공모전 정책 - 실시간 호출만 허용).
 */
@Component
@RequiredArgsConstructor
public class TourApiClient {

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "PawPass";

    private final WebClient tourApiWebClient;

    @Value("${external-api.tour-api.service-key}")
    private String serviceKey;


    public TourAreaBasedListResponse areaBasedList(
            Integer numOfRows,
            Integer pageNo,
            String arrange,
            String contentTypeId,
            String lDongRegnCd,
            String lDongSignguCd,
            String lclsSystm1,
            String lclsSystm2,
            String lclsSystm3
    ) {
        return tourApiWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/areaBasedList2")
                            .queryParam("serviceKey", decodedServiceKey())
                            .queryParam("MobileOS", MOBILE_OS)
                            .queryParam("MobileApp", MOBILE_APP)
                            .queryParam("_type", "json");
                    if (numOfRows != null) uriBuilder.queryParam("numOfRows", numOfRows);
                    if (pageNo != null) uriBuilder.queryParam("pageNo", pageNo);
                    if (arrange != null) uriBuilder.queryParam("arrange", arrange);
                    if (contentTypeId != null) uriBuilder.queryParam("contentTypeId", contentTypeId);
                    if (lDongRegnCd != null) uriBuilder.queryParam("lDongRegnCd", lDongRegnCd);
                    if (lDongSignguCd != null) uriBuilder.queryParam("lDongSignguCd", lDongSignguCd);
                    if (lclsSystm1 != null) uriBuilder.queryParam("lclsSystm1", lclsSystm1);
                    if (lclsSystm2 != null) uriBuilder.queryParam("lclsSystm2", lclsSystm2);
                    if (lclsSystm3 != null) uriBuilder.queryParam("lclsSystm3", lclsSystm3);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(TourAreaBasedListResponse.class)
                .block();
    }


    private String decodedServiceKey() {
        return URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);
    }
}
