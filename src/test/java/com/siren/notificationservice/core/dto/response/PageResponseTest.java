package com.siren.notificationservice.core.dto.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void fromMapsSpringPageFieldsToWireContract() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 2);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    void fromReflectsMiddlePage() {
        PageImpl<String> page = new PageImpl<>(List.of("c"), PageRequest.of(1, 1), 3);

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }
}
