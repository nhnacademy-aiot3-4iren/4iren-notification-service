package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.request.RecommendationRequest;
import com.siren.notificationservice.core.dto.response.RecommendationResponse;
import org.springframework.stereotype.Component;

@Component
public class RecommendationApiFallback implements RecommendationApiClient {

    @Override
    public RecommendationResponse getRecommendation(Long userId, RecommendationRequest recommendationRequest) {
        return new RecommendationResponse(
                userId,
                null,
                recommendationRequest.question(),
                "AI가 지금 바빠요 잠시후에 시도해주세요",
                recommendationRequest.requestedAt(),
                null,
                null
        );
    }
}