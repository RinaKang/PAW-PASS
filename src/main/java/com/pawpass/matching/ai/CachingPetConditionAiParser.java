package com.pawpass.matching.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pawpass.matching.ai.gemini.GeminiPetConditionAiParser;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * {@link PetConditionAiParser} 앞단에 붙는 캐싱 데코레이터.
 * TourAPI 원문 자체는 이 캐시와 무관하게 매 요청마다 계속 실시간으로 호출한다 (캐싱 금지 정책은
 * MatchingService가 매번 TourService.getDetail()을 부르는 것으로 그대로 지킨다) - 여기서 캐싱하는 건
 * 그 원문에서 뽑아낸 Gemini 판정 결과뿐이다. 원문(rawText)이 캐시 키라서, 원문이 바뀌면(운영자가 조건
 * 문구를 수정하는 등) 자동으로 캐시 미스가 나서 다시 판정된다 - 별도 무효화 로직이 필요 없다.
 */
@Primary
@Component
public class CachingPetConditionAiParser implements PetConditionAiParser {

    private static final int MAX_ENTRIES = 2000;
    private static final Duration TTL = Duration.ofHours(6);

    private final PetConditionAiParser delegate;
    private final Cache<String, ParsedCondition> cache;

    public CachingPetConditionAiParser(GeminiPetConditionAiParser delegate) {
        this.delegate = delegate;
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_ENTRIES)
                .expireAfterWrite(TTL)
                .build();
    }

    @Override
    public ParsedCondition parse(String rawText) {
        return cache.get(rawText, delegate::parse);
    }
}
