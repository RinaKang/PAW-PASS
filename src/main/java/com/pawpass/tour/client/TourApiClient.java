package com.pawpass.tour.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawpass.tour.dto.external.TourAreaBasedListResponse;
import com.pawpass.tour.dto.external.TourDetailCommonItem;
import com.pawpass.tour.dto.external.TourDetailImageItem;
import com.pawpass.tour.dto.external.TourDetailIntroItem;
import com.pawpass.tour.dto.external.TourDetailPetTourItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

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
    private final ObjectMapper objectMapper;

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
            String lclsSystm3,
            String cat1,
            String cat2,
            String cat3
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
                    // cat1/cat2/cat3: TourAPI 구(舊) 분류체계 - lclsSystm과 달리 음식점(39) 안에서도
                    // 카페/찻집(A05020900)과 일반 음식점(A05020100 한식 등)을 실제로 구분해서 필터링해준다
                    // (2026-09-12 실측 확인: lclsSystm3는 카페/일반 음식점이 섞여서 나와 구분이 안 됐음).
                    if (cat1 != null) uriBuilder.queryParam("cat1", cat1);
                    if (cat2 != null) uriBuilder.queryParam("cat2", cat2);
                    if (cat3 != null) uriBuilder.queryParam("cat3", cat3);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(TourAreaBasedListResponse.class)
                .block();
    }


    public TourDetailCommonItem detailCommon(String contentId) {
        // contentId만 넘긴다 - defaultYN/firstImageYN/addrinfoYN/overviewYN 같은 필터 파라미터는
        // 이 API 버전에 없고, 특히 defaultYN을 보내면 INVALID_REQUEST_PARAMETER_ERROR로 요청 자체가 거부된다
        // (실제 키로 라이브 호출해서 확인함, 2026-09-10). contentId만 넘겨도 overview 등 전체 필드가 내려온다.
        return fetchDetailItem("/detailCommon2",
                uriBuilder -> uriBuilder.queryParam("contentId", contentId),
                TourDetailCommonItem.class);
    }

    public TourDetailIntroItem detailIntro(String contentId, String contentTypeId) {
        return fetchDetailItem("/detailIntro2",
                uriBuilder -> uriBuilder
                        .queryParam("contentId", contentId)
                        .queryParam("contentTypeId", contentTypeId),
                TourDetailIntroItem.class);
    }

    public TourDetailPetTourItem detailPetTour(String contentId) {
        return fetchDetailItem("/detailPetTour2",
                uriBuilder -> uriBuilder.queryParam("contentId", contentId),
                TourDetailPetTourItem.class);
    }

    /** 관광지당 이미지가 여러 장(0~N)이라 단일 item이 아니라 리스트로 받는다 - 없으면 빈 리스트. */
    public List<TourDetailImageItem> detailImage(String contentId) {
        return fetchDetailItems("/detailImage2",
                uriBuilder -> uriBuilder.queryParam("contentId", contentId).queryParam("imageYN", "Y"),
                TourDetailImageItem.class);
    }

    /**
     * detailCommon2/detailIntro2/detailPetTour2는 응답 구조(response.body.items.item)는 같고
     * 필드만 다르므로 JsonNode로 받아 item 하나만 골라낸 뒤 원하는 타입으로 변환한다.
     * areaBasedList2와 마찬가지로 결과 없음은 items가 빈 문자열로, 결과 1건은 item이 배열이 아닌
     * 객체 하나로 내려오는 경우가 있어 두 경우 모두 방어한다.
     */
    private <T> T fetchDetailItem(String path, UnaryOperator<UriBuilder> extraParams, Class<T> itemType) {
        JsonNode itemNode = fetchItemsNode(path, extraParams);
        if (itemNode == null) {
            return null;
        }
        if (itemNode.isArray()) {
            if (itemNode.isEmpty()) {
                return null;
            }
            itemNode = itemNode.get(0);
        }
        return objectMapper.convertValue(itemNode, itemType);
    }

    /** fetchDetailItem과 같은 응답 구조지만(detailImage2), item이 여러 개일 수 있어 전부 리스트로 모은다. */
    private <T> List<T> fetchDetailItems(String path, UnaryOperator<UriBuilder> extraParams, Class<T> itemType) {
        JsonNode itemNode = fetchItemsNode(path, extraParams);
        if (itemNode == null) {
            return List.of();
        }
        if (!itemNode.isArray()) {
            return List.of(objectMapper.convertValue(itemNode, itemType));
        }
        List<T> items = new ArrayList<>();
        for (JsonNode node : itemNode) {
            items.add(objectMapper.convertValue(node, itemType));
        }
        return items;
    }

    /** response.body.items.item 노드까지 내려가서 반환 - 결과 없음(빈 문자열/누락)이면 null. */
    private JsonNode fetchItemsNode(String path, UnaryOperator<UriBuilder> extraParams) {
        JsonNode root = tourApiWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path)
                            .queryParam("serviceKey", decodedServiceKey())
                            .queryParam("MobileOS", MOBILE_OS)
                            .queryParam("MobileApp", MOBILE_APP)
                            .queryParam("_type", "json");
                    return extraParams.apply(uriBuilder).build();
                })
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (root == null) {
            return null;
        }
        JsonNode itemNode = root.path("response").path("body").path("items").path("item");
        if (itemNode.isMissingNode() || itemNode.isNull() || itemNode.isTextual()) {
            return null;
        }
        return itemNode;
    }

    private String decodedServiceKey() {
        return URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);
    }
}
