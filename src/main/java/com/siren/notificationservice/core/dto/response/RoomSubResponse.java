package com.siren.notificationservice.core.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoomSubResponse(
            @JsonProperty("roomId") Long roomId,
            @JsonProperty("roomName")String roomName,
            @JsonProperty("notificationEnabled") boolean notificationEnabled) {
    }