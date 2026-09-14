package com.pawpass.global.config;

import com.pawpass.auth.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 전부 JWT 기반 stateless 인증.
 * TODO: cors.allowed-origins 는 실제 프론트엔드 배포 URL 확정되면 application.yml에서 교체
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PERMIT_ALL_PATHS = {
            "/auth/google_id",
            "/auth/refresh",
            "/auth/logout",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            // 관광지/시설 탐색은 로그인 없이도 가능해야 함(비로그인 브라우징) - 원래 여기 없었던 게 버그였음
            // (2026-09-11, 마셍 리포트). /explore는 petId 없이 호출되면 원래도 전부 "확인필요"로 개인화
            // 없이 동작하도록 이미 설계돼 있어서 비로그인 접근과 자연스럽게 맞음.
            // 단 "/tours/*"·"/facilities/*"는 한 단계 경로만 매칭해서 "/tours/{id}/match" 같은 개인화
            // 엔드포인트(펫 정보 다룸이라 로그인 필수)까지 같이 풀리지 않게 함 - "/**"를 쓰면 안 됨.
            "/tours",
            "/tours/*",
            "/facilities",
            "/facilities/*",
            // /facilities/{id}/image(2026-09-12 추가)도 사진 자체는 개인화 정보가 아니라 같은 원칙으로
            // 공개 - "/match"까지 같이 풀리지 않게 "/image" 한 단계만 정확히 매칭한다.
            "/facilities/*/image",
            "/explore",
            // 업로드된 프로필 이미지(2026-09-15 추가) - 브라우저 <img> 태그는 Authorization 헤더를 안 붙이므로
            // 다른 사용자 화면에서도 그냥 로드되려면 공개여야 한다(/facilities/*/image와 같은 원칙).
            "/uploads/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:http://localhost:3000}") String allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
