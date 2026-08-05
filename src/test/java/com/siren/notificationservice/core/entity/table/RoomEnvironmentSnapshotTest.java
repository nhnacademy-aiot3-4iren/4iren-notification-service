package com.siren.notificationservice.core.entity.table;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RoomEnvironmentSnapshotTest {

    @Test
    void builderSetsAllFields() {
        ZonedDateTime windowStart = ZonedDateTime.now();

        RoomEnvironmentSnapshot snapshot = RoomEnvironmentSnapshot.builder()
                .roomId(7L)
                .windowStart(windowStart)
                .build();

        assertThat(snapshot.getRoomId()).isEqualTo(7L);
        assertThat(snapshot.getWindowStart()).isEqualTo(windowStart);
    }
}
