package com.pawpass.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 관광공사 TourAPI, 한국문화정보원 KCISA API, Gemini API 호출용 WebClient
 * TourAPI는 실시간 호출만 허용되므로
 * 반드시 tour 패키지의 서비스 레이어에서 매 요청마다 직접 호출해야 함
 *
 * WebClient 기본 인메모리 버퍼 한도(256KB)를 늘려둠 - 필드 많은 응답을 numOfRows/perPage 크게 잡아 호출하면
 * 기본값으로는 DataBufferLimitException이 남 (KCISA perPage=239 이상에서 실측 확인됨).
 *
 * 커넥션 풀 유휴 시간 제한(2026-09-16 추가): 기본 Reactor Netty 커넥션 풀은 유휴 커넥션을 오래 물고 있는데,
 * 외부 서버/게이트웨이 쪽이 그보다 먼저 그 커넥션을 조용히 끊어버리면 우리 쪽은 그걸 모른 채 재사용하다가
 * 요청이 실패한다 - 개발 서버를 며칠씩 안 내리고 계속 띄워둔 상태에서 실측으로 재현됨(TourAPI 자체는
 * 멀쩡한데 오래 켜둔 프로세스에서만 /tours/{id}가 500, 방금 새로 띄운 프로세스는 200). 배포 후에도 매번
 * 재시작으로 넘어갈 수 없으니, 원격이 끊기 전에 우리가 먼저 유휴 커넥션을 정리하도록 상한을 짧게 둔다.
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10MB

    // 대부분의 공공/상용 API 게이트웨이가 유휴 커넥션을 30~60초 안에 끊는 걸 감안해, 그보다 확실히 짧게 잡는다.
    private static final Duration CONNECTION_MAX_IDLE_TIME = Duration.ofSeconds(20);
    // 유휴 여부와 무관하게, 살아있는 커넥션도 이 시간이 지나면 강제로 새로 맺는다 - 장시간 재사용된 커넥션이
    // 중간 어딘가(로드밸런서 등)에서 알아채지 못하게 끊기는 경우까지 방어하는 2차 안전장치.
    private static final Duration CONNECTION_MAX_LIFE_TIME = Duration.ofMinutes(5);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final ObjectMapper objectMapper;
    private final AtomicInteger poolNameSuffix = new AtomicInteger();

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
                .clientConnector(reactorClientHttpConnector())
                .exchangeStrategies(appObjectMapperExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient kcisaApiWebClient() {
        return WebClient.builder()
                .baseUrl(kcisaApiBaseUrl)
                .clientConnector(reactorClientHttpConnector())
                .exchangeStrategies(appObjectMapperExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient geminiApiWebClient() {
        return WebClient.builder()
                .baseUrl(geminiApiBaseUrl)
                .clientConnector(reactorClientHttpConnector())
                .exchangeStrategies(appObjectMapperExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient googlePlacesWebClient() {
        return WebClient.builder()
                .baseUrl(googlePlacesBaseUrl)
                .clientConnector(reactorClientHttpConnector())
                .exchangeStrategies(appObjectMapperExchangeStrategies())
                .build();
    }

    /**
     * WebClient 빈마다 이 메서드를 한 번씩 호출해서 각자 자기 전용 커넥션 풀을 갖는다(외부 서비스 5개,
     * 풀도 5개 - 서로 다른 원격지라 풀을 공유할 이유가 없다). 매번 새 ConnectionProvider를 만들기 때문에
     * 풀 이름에 호출마다 겹치지 않게 접미사를 붙인다.
     */
    private ReactorClientHttpConnector reactorClientHttpConnector() {
        ConnectionProvider provider = ConnectionProvider.builder("pawpass-external-api-" + poolNameSuffix.incrementAndGet())
                .maxIdleTime(CONNECTION_MAX_IDLE_TIME)
                .maxLifeTime(CONNECTION_MAX_LIFE_TIME)
                .build();
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis());
        return new ReactorClientHttpConnector(httpClient);
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
