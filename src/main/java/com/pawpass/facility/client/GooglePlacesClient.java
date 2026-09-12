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

    /**
     * 시설명+주소로 검색해 place_id 하나를 찾는다. 정상 응답인데 결과가 없으면 null을 반환한다("찾아봤지만
     * 없음" - 호출부가 이걸 캐싱해서 다음부턴 재시도를 안 하는 근거로 쓸 수 있음). 반면 호출 자체가
     * 실패하면(네트워크 오류, 레이트리밋 등) 예외를 그대로 던진다 - "못 찾음"과 "일시적으로 실패함"을
     * 구분해야 호출부가 후자를 영구 캐싱하지 않고 다음 조회 때 재시도할 수 있다.
     */
    public String searchPlaceId(String query) {
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
    }

    /**
     * place_id로 대표사진 1장의 실시간 URL + 저작자 표시를 가져온다 (Place Details -> Photo Media 2단계
     * 호출). 구글 정책상 사진을 노출하는 화면에는 저작자 표시(authorAttributions)를 같이 보여줘야 해서
     * URL만 반환하지 않고 attribution까지 같이 묶어서 반환한다.
     */
    public PlacePhoto fetchPhoto(String placeId, int maxWidthPx) {
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
            GooglePlaceDetailsPhotosResponse.Photo photo = photos.get(0);
            String photoName = photo.name();

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
            if (media == null || media.photoUri() == null) {
                return null;
            }
            return new PlacePhoto(media.photoUri(), attributionText(photo.authorAttributions()));
        } catch (Exception e) {
            log.warn("Google Places Photo 조회 실패 - placeId={}, error={}", placeId, e.getMessage());
            return null;
        }
    }

    /** 저작자가 여럿이면 쉼표로 묶어서 하나의 표시 문구로 만든다. 저작자 정보가 아예 없으면 null(표시 생략). */
    private String attributionText(List<GooglePlaceDetailsPhotosResponse.AuthorAttribution> authorAttributions) {
        if (authorAttributions == null || authorAttributions.isEmpty()) {
            return null;
        }
        String joined = authorAttributions.stream()
                .map(GooglePlaceDetailsPhotosResponse.AuthorAttribution::displayName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
        return (joined == null || joined.isBlank()) ? null : joined;
    }

    public record PlacePhoto(String url, String attribution) {
    }
}
