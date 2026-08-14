package com.siren.notificationservice.core.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/** Recommendation 폴백 진입 시 실제 원인 예외를 로깅한다. */
@Slf4j
@Component
public class RecommendationApiFallbackFactory implements FallbackFactory<RecommendationApiClient> {

    @Override
    public RecommendationApiClient create(Throwable cause) {
        log.error("[Recommendation] 폴백 진입 - 원인: {}", cause.toString(), cause);
        return new RecommendationApiFallback();
    }
}
