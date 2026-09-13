package com.pawpass.global.exception;

import com.pawpass.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // 2026-09-13: /tours/{id}/match를 petId 없이 호출하면 이 예외가 던져지는데, 원래 안 잡혀 있어서
    // 500(내부 오류)으로 새던 실제 버그 - 프론트 콘솔 로그로 발견됨. 400으로 깔끔하게 내려가야 한다.
    @Test
    void 필수_쿼리파라미터_누락은_500이_아니라_400으로_처리된다() {
        MissingServletRequestParameterException e =
                new MissingServletRequestParameterException("petId", "Long");

        ResponseEntity<ApiResponse<Object>> response = handler.handleMissingParam(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("petId");
    }

    // 2026-09-13: POST /favorites에 source="tourapi"(소문자)를 보냈다가 DataSource enum이 대문자만
    // 받아서 500이 났던 실제 사례 - DataSource 자체는 @JsonCreator로 따로 고쳤지만, 비슷한 유형의
    // "본문이 DTO와 안 맞음" 문제 전반에 대한 방어망도 같이 넣는다.
    @Test
    void 요청_본문_파싱_실패는_500이_아니라_400으로_처리된다() {
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("파싱 실패", (org.springframework.http.HttpInputMessage) null);

        ResponseEntity<ApiResponse<Object>> response = handler.handleMalformedBody(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // 2026-09-13: TourAPI 서비스 키가 일일 요청 한도를 초과해 detailCommon2가 HTTP 429를 반환했는데,
    // 이걸 안 잡아둬서 GET /tours/{contentId}가 그대로 500으로 새던 실제 사례(live-verified).
    @Test
    void 외부_API_호출_실패는_500이_아니라_503으로_처리된다() {
        WebClientResponseException e = WebClientResponseException.create(
                429, "Too Many Requests", null, "쿼터 초과".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        ResponseEntity<ApiResponse<Object>> response = handler.handleExternalApiFailure(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}
