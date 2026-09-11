package com.pawpass.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 관광공사 TourAPI, 한국문화정보원 KCISA API, Gemini API 호출용 WebClient
 * TourAPI는 실시간 호출만 허용되므로
 * 반드시 tour 패키지의 서비스 레이어에서 매 요청마다 직접 호출해야 함
 *
 * WebClient 기본 인메모리 버퍼 한도(256KB)를 늘려둠 - 필드 많은 응답을 numOfRows/perPage 크게 잡아 호출하면
 * 기본값으로는 DataBufferLimitException이 남 (KCISA perPage=239 이상에서 실측 확인됨).
 *
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10MB

    private final ObjectMapper objectMapper;

    @Value("${external-api.tour-api.base-url}")
    private String tourApiBaseUrl;

    @Value("${external-api.kcisa-api.base-url}")
    private String kcisaApiBaseUrl;

    @Value("${external-api.gemini-api.base-url}")
    private String geminiApiBaseUrl;

    @Value("${external-api.google-places.base-url}")
    private String googlePlacesBaseUrl;

    @Bean
    public WebClient tourApiWebClient() {
        return WebClient.builder()
                .baseUrl(tourApiBaseUrl)
                .exchangeStrategies(appObjectMapperExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient kcisaApiWebClient() {
        return WebClient.builder()
                .baseUrl(kcisaApiBaseUrl)
                .exchangeStrategies(appObjectMapperExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient geminiApiWebClient() {
        return WebClient.builder()
                .baseUrl(geminiApiBaseUrl)
                .exchangeStrategies(appObjectMapperExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient googlePlacesWebClient() {
        return WebClient.builder()
                .baseUrl(googlePlacesBaseUrl)
                .exchangeStrategies(appObjectMapperExchangeStrategies())
                .build();
    }

    private ExchangeStrategies appObjectMapperExchangeStrategies() {
        return ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE);
                    configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
                    configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
                })
                .build();
    }
}
