package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.request.RecommendationRequest;
import com.siren.notificationservice.core.dto.response.RecommendationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "4IREN-RECOMMENDATION", contextId = "recommendationApi", fallback = RecommendationApiFallback.class)
public interface RecommendationApiClient {

    @PostMapping("/api/recommendation/chat")
    RecommendationResponse getRecommendation(@RequestHeader("X-USER-ID") Long userId,
                                              @RequestBody RecommendationRequest recommendationRequest);
}
