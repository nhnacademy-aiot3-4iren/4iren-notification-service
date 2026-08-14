package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.request.RecommendationRequest;
import com.siren.notificationservice.core.dto.response.RecommendationResponse;
import com.siren.notificationservice.core.entity.domain.UserRole;

import java.util.List;

public class RecommendationApiFallback implements RecommendationApiClient {

    @Override
    public RecommendationResponse getRecommendation(Long userId, UserRole userRole, String serviceType, RecommendationRequest recommendationRequest) {

        return new RecommendationResponse(
                userId,
                null,
                recommendationRequest.message(),
                new RecommendationResponse.Answer("AI가 지금 바빠요 잠시후에 시도해주세요", List.of()),
                recommendationRequest.requestedAt(),
                null,
                null
        );
    }
}