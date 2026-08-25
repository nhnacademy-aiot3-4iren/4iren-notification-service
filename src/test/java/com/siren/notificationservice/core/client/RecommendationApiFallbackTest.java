package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.request.RecommendationRequest;
import com.siren.notificationservice.core.dto.response.RecommendationResponse;
import com.siren.notificationservice.core.entity.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationApiFallbackTest {
    private final RecommendationApiFallback fallback = new RecommendationApiFallback();

    @Test
    void getRecommendationThrowsError(){
        Long userId = 1L;

        RecommendationRequest recommendationRequest = new RecommendationRequest(
                Collections.emptyList(),
                "아무거나",
                LocalDateTime.now()
        );
        RecommendationResponse response = fallback.getRecommendation(userId, UserRole.NORMAL, "TELEGRAM", recommendationRequest);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.message()).isEqualTo("아무거나"); // 기존 request의 메시지가 잘 들어갔는지 확인
        assertThat(response.answer().answer()).isEqualTo("AI가 지금 바빠요 잠시후에 시도해주세요");
    }

}