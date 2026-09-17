package com.pawpass.global.exception;

import com.pawpass.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
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

    // 2026-09-16: WebClientResponseException(비-2xx 응답)과 달리, 연결 자체가 실패했을 때(타임아웃,
    // 커넥션 리셋 등) 던져지는 별개 예외 타입 - 오래 떠있던 개발 서버의 WebClient 커넥션 풀에 죽은
    // 커넥션이 남아있다가 이걸로 터지는 걸 실측(WebClientConfig의 커넥션 풀 유휴시간 제한이 근본 대응).
    @Test
    void 외부_API_연결_자체_실패는_500이_아니라_503으로_처리된다() {
        WebClientRequestException e = new WebClientRequestException(
                new java.io.IOException("Connection reset by peer"), HttpMethod.GET,
                URI.create("https://apis.data.go.kr/some-endpoint"), new org.springframework.http.HttpHeaders());

        ResponseEntity<ApiResponse<Object>> response = handler.handleExternalApiConnectionFailure(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // 2026-09-15: 프로필 이미지 업로드(POST /users/me/profile-image, multipart/form-data 전용)를 JSON이나
    // 빈 본문으로 호출하면 이 예외가 던져지는데, 원래 안 잡혀 있어서 500으로 새던 실제 버그
    // (라이브 테스트로 발견, 다른 누락 예외 핸들러들과 같은 유형).
    @Test
    void 지원하지_않는_Content_Type은_500이_아니라_400으로_처리된다() {
        HttpMediaTypeNotSupportedException e = new HttpMediaTypeNotSupportedException("Content-Type is not supported");

        ResponseEntity<ApiResponse<Object>> response = handler.handleUnsupportedMediaType(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // 2026-09-15: multipart 요청인데 필수 파트("image")가 아예 빠졌을 때 - 위와 같은 라이브 테스트에서
    // 함께 발견된 500 버그.
    @Test
    void 필수_멀티파트_파트_누락은_500이_아니라_400으로_처리된다() {
        MissingServletRequestPartException e = new MissingServletRequestPartException("image");

        ResponseEntity<ApiResponse<Object>> response = handler.handleMissingPart(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("image");
    }

    @Test
    void 업로드_용량_초과는_500이_아니라_400으로_처리된다() {
        MaxUploadSizeExceededException e = new MaxUploadSizeExceededException(5L * 1024 * 1024);

        ResponseEntity<ApiResponse<Object>> response = handler.handleUploadTooLarge(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    // 2026-09-15: 프로필 이미지 정적 서빙(/uploads/**)을 붙이면서 처음 발견 - 없는 파일을 요청하면
    // Spring 6.1+가 이 예외를 던지는데 안 잡혀 있으면 500으로 샌다(라이브 테스트로 발견). 파일이 없을
    // 뿐이니 404가 맞다.
    @Test
    void 존재하지_않는_정적_리소스는_500이_아니라_404로_처리된다() {
        NoResourceFoundException e = new NoResourceFoundException(HttpMethod.GET, "profile-images/missing.jpg");

        ResponseEntity<ApiResponse<Object>> response = handler.handleNoResourceFound(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}
