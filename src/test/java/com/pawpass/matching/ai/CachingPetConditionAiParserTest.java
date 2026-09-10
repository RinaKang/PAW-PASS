package com.pawpass.matching.ai;

import com.pawpass.matching.ai.gemini.GeminiPetConditionAiParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingPetConditionAiParserTest {

    @Mock
    private GeminiPetConditionAiParser delegate;

    @Test
    void 같은_원문은_한_번만_델리게이트를_호출한다() {
        CachingPetConditionAiParser parser = new CachingPetConditionAiParser(delegate);
        PetConditionAiParser.ParsedCondition parsed =
                new PetConditionAiParser.ParsedCondition(true, false, null, null, "", "", 0.9);
        when(delegate.parse("소형견만 가능")).thenReturn(parsed);

        PetConditionAiParser.ParsedCondition first = parser.parse("소형견만 가능");
        PetConditionAiParser.ParsedCondition second = parser.parse("소형견만 가능");

        assertThat(first).isEqualTo(parsed);
        assertThat(second).isEqualTo(parsed);
        verify(delegate, times(1)).parse("소형견만 가능");
    }

    @Test
    void 원문이_다르면_각각_델리게이트를_호출한다() {
        CachingPetConditionAiParser parser = new CachingPetConditionAiParser(delegate);
        when(delegate.parse("A")).thenReturn(new PetConditionAiParser.ParsedCondition(true, false, null, null, "", "", 0.9));
        when(delegate.parse("B")).thenReturn(new PetConditionAiParser.ParsedCondition(false, true, null, null, "", "", 0.9));

        parser.parse("A");
        parser.parse("B");

        verify(delegate, times(1)).parse("A");
        verify(delegate, times(1)).parse("B");
    }
}
