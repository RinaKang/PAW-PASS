package com.pawpass.route.service;

import com.pawpass.global.util.DataSource;
import com.pawpass.route.domain.RoutePlace;
import com.pawpass.route.dto.RoutePlaceRequest;
import com.pawpass.route.dto.RoutePlaceResponse;
import com.pawpass.route.repository.RoutePlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavedRouteServiceTest {

    @Mock
    private RoutePlaceRepository routePlaceRepository;

    @InjectMocks
    private SavedRouteService savedRouteService;

    @Test
    void 저장된_동선이_없으면_빈_목록을_반환한다() {
        when(routePlaceRepository.findAllByUserIdOrderBySortOrderAsc(1L)).thenReturn(List.of());

        List<RoutePlaceResponse> result = savedRouteService.findAll(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void 저장된_동선을_정렬순서대로_반환한다() {
        RoutePlace first = place(1L, DataSource.TOURAPI, "t1", "제목1", 0);
        RoutePlace second = place(2L, DataSource.KCISA, "f1", "제목2", 1);
        when(routePlaceRepository.findAllByUserIdOrderBySortOrderAsc(1L)).thenReturn(List.of(first, second));

        List<RoutePlaceResponse> result = savedRouteService.findAll(1L);

        assertThat(result).extracting(RoutePlaceResponse::contentId).containsExactly("t1", "f1");
    }

    // 추가/삭제/순서변경을 구분하지 않고 프론트가 보낸 배열을 그대로 새 상태로 취급한다.
    @Test
    void 전체_교체는_기존_목록을_지우고_배열_순서대로_sortOrder를_다시_매긴다() {
        when(routePlaceRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        List<RoutePlaceRequest> requests = List.of(
                new RoutePlaceRequest(DataSource.TOURAPI, "t1", "제목1", 1.0, 1.0),
                new RoutePlaceRequest(DataSource.KCISA, "f1", "제목2", 2.0, 2.0)
        );

        List<RoutePlaceResponse> result = savedRouteService.replaceAll(1L, requests);

        verify(routePlaceRepository).deleteAllByUserId(1L);
        assertThat(result).extracting(RoutePlaceResponse::contentId).containsExactly("t1", "f1");

        ArgumentCaptor<List<RoutePlace>> captor = ArgumentCaptor.forClass(List.class);
        verify(routePlaceRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(RoutePlace::getSortOrder).containsExactly(0, 1);
        assertThat(captor.getValue()).extracting(RoutePlace::getUserId).containsExactly(1L, 1L);
    }

    @Test
    void 빈_배열로_교체하면_기존_목록만_지우고_아무것도_저장하지_않는다() {
        when(routePlaceRepository.saveAll(eq(List.of()))).thenReturn(List.of());

        List<RoutePlaceResponse> result = savedRouteService.replaceAll(1L, List.of());

        verify(routePlaceRepository).deleteAllByUserId(1L);
        assertThat(result).isEmpty();
    }

    private RoutePlace place(long userId, DataSource source, String contentId, String title, int sortOrder) {
        return RoutePlace.builder()
                .userId(userId).source(source).contentId(contentId).title(title)
                .lat(1.0).lng(1.0).sortOrder(sortOrder).build();
    }
}
