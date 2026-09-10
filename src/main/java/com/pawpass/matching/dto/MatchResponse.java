package com.pawpass.matching.dto;


public record MatchResponse(String status, String reason, String rawText) {

    public static final String STATUS_ALLOWED = "가능";
    public static final String STATUS_CONDITIONAL = "조건부";
    public static final String STATUS_DENIED = "불가";
    public static final String STATUS_UNKNOWN = "확인필요";
}
