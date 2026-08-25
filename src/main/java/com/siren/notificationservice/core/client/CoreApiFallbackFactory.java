package com.siren.notificationservice.core.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CoreApiFallbackFactory  implements FallbackFactory<CoreApiClient> {
    @Override
    public CoreApiClient create(Throwable cause) {
        log.error("[Core] 폴백 진입 - 원인: {}", cause.toString(), cause);
        return new CoreApiClientFallback();
    }
}
