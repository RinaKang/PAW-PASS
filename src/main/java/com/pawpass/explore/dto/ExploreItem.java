package com.pawpass.explore.dto;

import com.pawpass.facility.dto.FacilitySummaryResponse;
import com.pawpass.tour.dto.TourSummaryResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public record ExploreItem(
        String source,
        String id,
        String title,
        String addr,
        Double lat,
        Double lng,
        String dedupKey,
        String matchStatus
) {
    public static ExploreItem fromTour(TourSummaryResponse tour, String matchStatus) {
        // TourAPI 좌표 표기 관례: mapX=경도(longitude), mapY=위도(latitude)
        return new ExploreItem("tourapi", tour.contentId(), tour.title(), tour.addr(),
                tour.mapY(), tour.mapX(), dedupKey(tour.title()), matchStatus);
    }

    public static ExploreItem fromFacility(FacilitySummaryResponse facility, String matchStatus) {
        return new ExploreItem("kcisa", facility.id(), facility.title(), facility.addr(),
                facility.lat(), facility.lng(), dedupKey(facility.title()), matchStatus);
    }

    public ExploreItem withMatchStatus(String newMatchStatus) {
        return new ExploreItem(source, id, title, addr, lat, lng, dedupKey, newMatchStatus);
    }

    /**
     * 좌표가 둘 다 있으면 ExploreService가 실제로는 이 값 대신 좌표 근접도로 중복을 판단한다 - dedupKey는
     * 좌표가 없는 항목(간혹 TourAPI mapx/mapy가 빈 값으로 오는 경우)에 대한 폴백, 그리고 응답 필드(dedup_key)
     * 자체가 명세에 있어서 항상 채워둔다.
     */
    private static String dedupKey(String title) {
        String normalized = title == null ? "" : title.replaceAll("\\s+", "").toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
