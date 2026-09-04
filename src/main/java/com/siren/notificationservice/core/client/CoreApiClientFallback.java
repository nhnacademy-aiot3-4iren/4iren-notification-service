package com.siren.notificationservice.core.client;

import com.siren.notificationservice.core.dto.response.OutsideWeather;
import com.siren.notificationservice.core.dto.response.RoomEnvironmentReadingResponse;
import com.siren.notificationservice.core.dto.response.RoomSubscribersResponse;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.exception.CoreApiUnavailableException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CoreApiClientFallback implements CoreApiClient {
    private static final String USER_ID="userId";
    private static final String ROOM_ID="roomId";

    @Override
    public UserRoomSubResponse getRoomSubscriptions(Long userId) {
        throw new CoreApiUnavailableException(userId, USER_ID);
    }

    @Override
    public RoomSubscribersResponse getSubscribers(Long roomId) {
        throw  new CoreApiUnavailableException(roomId, ROOM_ID);
    }

    @Override
    public RoomEnvironmentReadingResponse getRoomSensorsReadings(Long roomId, LocalDateTime requestAt) {
        throw  new CoreApiUnavailableException(roomId, ROOM_ID);
    }

    @Override
    public OutsideWeather getOutsideWeather(Long roomId, LocalDateTime requestAt) {
        throw  new CoreApiUnavailableException(roomId, ROOM_ID);
    }
}