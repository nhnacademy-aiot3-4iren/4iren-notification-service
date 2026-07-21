package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import org.springframework.stereotype.Component;

@Component
public class CoreApiClientFallback implements CoreApiClient {

    @Override
    public UserRoomSubResponse getRoomSubscriptions(Long userId) {
        throw new CoreApiUnavailableException(userId);
    }
}