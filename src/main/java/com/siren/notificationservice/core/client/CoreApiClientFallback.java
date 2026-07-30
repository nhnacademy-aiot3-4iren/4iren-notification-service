package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.request.RoomSensorsReadingRequest;
import com.siren.notificationservice.core.dto.response.OutsideWeather;
import com.siren.notificationservice.core.dto.response.RoomEnvironmentReadingResponse;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CoreApiClientFallback implements CoreApiClient {

    @Override
    public UserRoomSubResponse getRoomSubscriptions(Long userId) {
        throw new CoreApiUnavailableException(userId, "userId");
    }

    @Override
    public RoomEnvironmentReadingResponse getRoomSensorsReadings(RoomSensorsReadingRequest request) {
        throw  new CoreApiUnavailableException(request.roomId(), "roomId");
    }

    @Override
    public OutsideWeather getOutsideWeather(Long roomId, LocalDateTime requestAt) {
        throw  new CoreApiUnavailableException(roomId, "roomId");
    }
}