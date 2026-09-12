package com.pawpass.global.exception;

import com.pawpass.global.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
