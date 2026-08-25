package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.request.RecommendationRequest;
import com.siren.notificationservice.core.dto.response.RecommendationResponse;
import com.siren.notificationservice.core.entity.domain.UserRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "4IREN-RECOMMENDATION", contextId = "recommendationApi", fallbackFactory = RecommendationApiFallbackFactory.class)
public interface RecommendationApiClient {

    @PostMapping("/api/recommendation/chat")
    RecommendationResponse getRecommendation(@RequestHeader("X-USER-ID") Long userId,
                                              @RequestHeader("X-USER-ROLE") UserRole userRole,
                                              @RequestHeader("X-CLIENT-TYPE") String serviceType,
                                              @RequestBody RecommendationRequest recommendationRequest);
}
