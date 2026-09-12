package com.pawpass.global.exception;

import com.pawpass.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

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
}
