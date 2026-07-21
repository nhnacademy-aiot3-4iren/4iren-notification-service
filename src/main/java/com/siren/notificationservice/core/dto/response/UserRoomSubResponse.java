package com.siren.notificationservice.core.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UserRoomSubResponse(
        @JsonProperty("userId") Long userId,
        @JsonProperty("roomSubInfo")List<RoomSubResponse> roomSubInfo
) {
    public record RoomSubResponse(
            @JsonProperty("roomId") Long roomId,
            @JsonProperty("notificationEnabled") boolean notificationEnabled) {
    }
}
