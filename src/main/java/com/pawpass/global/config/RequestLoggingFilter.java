package com.pawpass.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 요청이 백엔드까지 도달하는지 자체를 눈으로 바로 확인하기 위한 최소 로깅.
 * Security 필터 체인보다 먼저 돌게 최우선 순위를 줘서, CORS/인증 때문에 막힌 요청도 빠짐없이 찍힌다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(request, response);
        log.info(">> {} {} -> {}", request.getMethod(), request.getRequestURI(), response.getStatus());
    }
}
