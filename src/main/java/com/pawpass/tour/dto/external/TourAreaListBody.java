package com.pawpass.tour.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.io.IOException;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourAreaListBody(
        @JsonDeserialize(using = ItemsDeserializer.class) Items items,
        Integer numOfRows,
        Integer pageNo,
        Integer totalCount
) {
    // TourAPI 계열은 결과 0건일 때 items가 "" (빈 문자열)로 오는 경우가 있고,
    // 결과가 정확히 1건일 때는 item이 배열이 아니라 객체 하나로 오는 경우도 있음.
    // ItemsDeserializer가 두 경우를 모두 빈 리스트/단일 원소 리스트로 안전하게 처리함.
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<TourAreaItem> item) {
    }

    static class ItemsDeserializer extends JsonDeserializer<Items> {
        @Override
        public Items deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode node = mapper.readTree(p);

            if (node == null || node.isNull() || node.isMissingNode() || node.isTextual()) {
                // 결과 0건: items가 "" (빈 문자열)로 내려오는 경우
                return new Items(List.of());
            }

            JsonNode itemNode = node.get("item");
            if (itemNode == null || itemNode.isNull() || itemNode.isMissingNode()) {
                return new Items(List.of());
            }

            if (itemNode.isArray()) {
                List<TourAreaItem> items = mapper.convertValue(itemNode, new TypeReference<List<TourAreaItem>>() {});
                return new Items(items);
            }

            // 결과 1건: item이 배열이 아니라 객체 하나로 내려오는 경우
            TourAreaItem single = mapper.convertValue(itemNode, TourAreaItem.class);
            return new Items(List.of(single));
        }
    }
}
