package com.pawpass.global.exception;

import com.pawpass.global.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    /**
     * 필수 @RequestParam이 아예 빠졌을 때(예: /tours/{id}/match를 petId 없이 호출) - 이걸 안 잡아두면
     * IllegalArgumentException이 아니라서 아래 handleUnexpected()로 떨어져 500이 나가버린다
     * (2026-09-13, 프론트 petId 누락 호출로 실제 500이 발생해서 발견됨). 요청 자체가 잘못된 거라 400이 맞다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getParameterName() + " 파라미터가 필요합니다."));
    }

    /** 파라미터 타입이 안 맞을 때(예: petId=abc처럼 숫자가 아닌 값) - 위와 같은 이유로 400 처리. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getName() + " 파라미터 형식이 올바르지 않습니다."));
    }

    /**
     * 요청 본문(JSON)이 DTO 형태와 안 맞을 때(필수 필드 누락, enum에 없는 문자열 등) - 2026-09-13,
     * POST /favorites에 source="tourapi"(소문자)를 보냈다가 DataSource enum이 대문자만 받아서 500이
     * 났던 실제 사례로 발견됨(그 자체는 DataSource에 @JsonCreator를 추가해 따로 고쳤지만, 비슷한 유형의
     * "본문이 DTO와 안 맞음" 문제가 또 생겨도 이제 500이 아니라 400으로 떨어지게 하는 방어망).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMalformedBody(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("요청 본문 형식이 올바르지 않습니다."));
    }

    /**
     * TourAPI/KCISA/Gemini/구글 Places 등 외부 API 호출 자체가 실패했을 때(비-2xx 응답) - WebClient의
     * 기본 동작은 이걸 예외로 던지는데, 안 잡아두면 그대로 handleUnexpected()로 떨어져 500이 나간다.
     * 2026-09-13 실사례로 발견: TourAPI 서비스 키가 일일 요청 한도(data.go.kr 쪽 제약)를 초과해서
     * detailCommon2가 HTTP 429(LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR)를 반환했는데, 이게
     * GET /tours/{contentId} 자체는 물론 PlaceLookupService를 거치는 GET /favorites, /trips까지
     * 500으로 전파시켰다(PlaceLookupService 쪽은 별도로 예외 처리 범위를 넓혀서 추가 방어함).
     * 클라이언트 잘못이 아니라 "지금 외부 서비스가 안 됨"이라 503이 맞다.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ApiResponse<Object>> handleExternalApiFailure(WebClientResponseException e) {
        log.warn("외부 API 호출 실패 - status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("외부 서비스 호출에 실패했습니다. 잠시 후 다시 시도해주세요."));
    }

    /**
     * 위와 달리 외부 서버가 응답(2xx든 에러든)을 아예 안 줬을 때 - 연결 자체가 실패했거나(타임아웃, 커넥션
     * 리셋 등) DNS 조회 실패 같은 경우. WebClientResponseException과는 상속 관계가 아닌 별개 타입이라
     * 따로 잡아야 한다(2026-09-16, 오래 떠있던 개발 서버의 WebClient 커넥션 풀에 죽은 커넥션이 남아있다가
     * 이 예외로 터지는 걸 실측 - WebClientConfig의 커넥션 풀 유휴시간 제한이 근본 대응이고, 이건 그래도
     * 남는 경우를 위한 방어망). 클라이언트 잘못이 아니므로 503이 맞다.
     */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleExternalApiConnectionFailure(WebClientRequestException e) {
        log.warn("외부 API 연결 실패 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("외부 서비스 연결에 실패했습니다. 잠시 후 다시 시도해주세요."));
    }

    /**
     * spring.servlet.multipart.max-file-size(5MB, 2026-09-15 프로필 이미지 업로드 추가 시 설정)를
     * 넘는 파일을 업로드했을 때 - ProfileImageStorage.validate()가 직접 잡는 크기 초과는
     * IllegalArgumentException으로 잡히지만, 서블릿 컨테이너 단에서 요청을 파싱하다가 먼저 걸리면
     * 컨트롤러까지 오지도 못하고 이 예외가 던져진다. 안 잡아두면 다른 예외들처럼 500으로 떨어진다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("파일 용량이 너무 큽니다."));
    }

    /**
     * POST /users/me/profile-image(consumes=multipart/form-data)를 그냥 JSON이나 빈 본문으로 호출했을 때
     * - 2026-09-15, 실제로 라이브 테스트하다가 500으로 떨어지는 걸 발견해서 추가(다른 누락된 예외 핸들러들과
     * 같은 유형의 문제 - Content-Type이 안 맞는 것도 클라이언트 잘못이라 400이 맞다).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("이 요청 형식은 지원하지 않습니다."));
    }

    /**
     * multipart 요청인데 필수 파트(예: profile-image 업로드의 "image")가 아예 빠졌을 때 - 위와 같은 라이브
     * 테스트에서 함께 발견됨. MissingServletRequestParameterException(쿼리 파라미터용)과 별개 예외 타입이라
     * 따로 잡아야 한다.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingPart(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(e.getRequestPartName() + " 파트가 필요합니다."));
    }

    /**
     * 존재하지 않는 정적 리소스 요청(예: /uploads/profile-images/{없는 파일명}) - 2026-09-15 프로필 이미지
     * 업로드 기능 추가하면서 /uploads/** 정적 서빙을 처음 붙였는데, 라이브 테스트 중 없는 파일을 요청하니
     * 500이 나가는 걸 발견함(Spring 6.1+는 이걸 404 HTML 에러 페이지가 아니라 예외로 던짐 - 안 잡아두면
     * 다른 누락 예외들처럼 그대로 500으로 떨어진다). 파일이 없는 것뿐이라 404가 맞다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("요청한 리소스를 찾을 수 없습니다."));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnsupported(UnsupportedOperationException e) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        // 처리되지 않은 예외는 반드시 로그를 남긴다 - 안 그러면 500만 뜨고 원인 추적이 불가능해짐.
        log.error("처리되지 않은 예외 발생", e);
        return ResponseEntity.internalServerError().body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
    }
}
