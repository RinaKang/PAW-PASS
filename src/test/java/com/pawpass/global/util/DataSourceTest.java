package com.pawpass.global.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataSourceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // /explore의 source 필드가 소문자 문자열("tourapi"/"kcisa")이라, 프론트가 그 값을 그대로
    // POST /favorites 등에 돌려보내는 흐름이 자연스럽게 동작해야 한다(2026-09-13, 이게 안 돼서 500 났었음).
    @Test
    void 소문자_문자열을_역직렬화할_수_있다() throws Exception {
        assertThat(objectMapper.readValue("\"tourapi\"", DataSource.class)).isEqualTo(DataSource.TOURAPI);
        assertThat(objectMapper.readValue("\"kcisa\"", DataSource.class)).isEqualTo(DataSource.KCISA);
    }

    @Test
    void 대문자_문자열도_역직렬화할_수_있다() throws Exception {
        assertThat(objectMapper.readValue("\"TOURAPI\"", DataSource.class)).isEqualTo(DataSource.TOURAPI);
        assertThat(objectMapper.readValue("\"KCISA\"", DataSource.class)).isEqualTo(DataSource.KCISA);
    }

    @Test
    void 직렬화는_소문자로_나간다() throws Exception {
        assertThat(objectMapper.writeValueAsString(DataSource.TOURAPI)).isEqualTo("\"tourapi\"");
        assertThat(objectMapper.writeValueAsString(DataSource.KCISA)).isEqualTo("\"kcisa\"");
    }

    @Test
    void 알수없는_값은_예외() {
        assertThatThrownBy(() -> objectMapper.readValue("\"naver\"", DataSource.class))
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
