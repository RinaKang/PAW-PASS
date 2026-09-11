package com.pawpass.facility.client;

import com.pawpass.facility.dto.external.GooglePlaceDetailsPhotosResponse;
import com.pawpass.facility.dto.external.GooglePlacePhotoMediaResponse;
import com.pawpass.facility.dto.external.GooglePlacesTextSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Google Places API (New) - KCISA 시설 대표사진용.
 * place_id(searchPlaceId 결과)는 장기 저장 가능하지만, 사진 이름/URL(fetchPhotoUrl 결과)은 구글 정책상
 * 캐싱이 금지되어 있어 호출부에서 절대 DB에 저장하지 말고 매 요청마다 다시 호출해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePlacesClient {

    private final WebClient googlePlacesWebClient;

    @Value("${external-api.google-places.api-key}")
    private String apiKey;

    /** 시설명+주소로 검색해 place_id 하나를 찾는다. 못 찾거나 실패하면 null (배치 동기화에서 스킵 처리). */
    public String searchPlaceId(String query) {
        try {
            GooglePlacesTextSearchResponse response = googlePlacesWebClient.post()
                    .uri("/v1/places:searchText")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "places.id")
                    .bodyValue(Map.of("textQuery", query, "languageCode", "ko"))
                    .retrieve()
                    .bodyToMono(GooglePlacesTextSearchResponse.class)
                    .block();
            List<GooglePlacesTextSearchResponse.Place> places = response == null ? null : response.places();
            if (places == null || places.isEmpty()) {
                return null;
            }
            return places.get(0).id();
        } catch (Exception e) {
            log.warn("Google Places Text Search 실패 - query={}, error={}", query, e.getMessage());
            return null;
        }
    }

    /** place_id로 대표사진 1장의 실시간 URL을 가져온다 (Place Details -> Photo Media 2단계 호출). */
    public String fetchPhotoUrl(String placeId, int maxWidthPx) {
        if (placeId == null || placeId.isBlank()) {
            return null;
        }
        try {
            GooglePlaceDetailsPhotosResponse details = googlePlacesWebClient.get()
                    .uri("/v1/places/{placeId}", placeId)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "photos")
                    .retrieve()
                    .bodyToMono(GooglePlaceDetailsPhotosResponse.class)
                    .block();
            List<GooglePlaceDetailsPhotosResponse.Photo> photos = details == null ? null : details.photos();
            if (photos == null || photos.isEmpty()) {
                return null;
            }
            String photoName = photos.get(0).name();

            GooglePlacePhotoMediaResponse media = googlePlacesWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            // photoName 자체에 "/"가 포함돼 있어 {템플릿} 변수로 넘기면 퍼센트 인코딩되어
                            // 경로가 깨진다 - path()에 직접 이어붙여 리터럴 "/"를 그대로 유지한다.
                            .path("/v1/" + photoName + "/media")
                            .queryParam("skipHttpRedirect", "true")
                            .queryParam("maxWidthPx", maxWidthPx)
                            .build())
                    .header("X-Goog-Api-Key", apiKey)
                    .retrieve()
                    .bodyToMono(GooglePlacePhotoMediaResponse.class)
                    .block();
            return media == null ? null : media.photoUri();
        } catch (Exception e) {
            log.warn("Google Places Photo 조회 실패 - placeId={}, error={}", placeId, e.getMessage());
            return null;
        }
    }
}
