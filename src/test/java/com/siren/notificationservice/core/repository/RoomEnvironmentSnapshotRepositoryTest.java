package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class RoomEnvironmentSnapshotRepositoryTest {

    @Autowired
    private RoomEnvironmentSnapshotRepository roomEnvironmentSnapshotRepository;

    @Test
    void findsFirstSnapshotCoveringReferenceTime() {
        ZonedDateTime windowStart = ZonedDateTime.now();
        roomEnvironmentSnapshotRepository.save(createRoomEnvironmentSnapshot(3L, windowStart));

        Optional<RoomEnvironmentSnapshot> found = roomEnvironmentSnapshotRepository
                .findFirstByRoomIdAndWindowStartBetweenOrderByWindowStartAsc(3L, windowStart, windowStart.plusMinutes(15));

        assertThat(found).isPresent();
        assertThat(found.get().getRoomId()).isEqualTo(3L);
    }

    @Test
    void findsNothingWhenReferenceTimeIsOutsideCoverage() {
        ZonedDateTime windowStart = ZonedDateTime.now();
        roomEnvironmentSnapshotRepository.save(createRoomEnvironmentSnapshot(3L, windowStart));

        Optional<RoomEnvironmentSnapshot> found = roomEnvironmentSnapshotRepository
                .findFirstByRoomIdAndWindowStartBetweenOrderByWindowStartAsc(
                        3L, windowStart.minusHours(1), windowStart.minusMinutes(46));

        assertThat(found).isEmpty();
    }

    @Test
    void findsAllSnapshotsForRoom() {
        roomEnvironmentSnapshotRepository.save(createRoomEnvironmentSnapshot(3L, ZonedDateTime.now()));
        roomEnvironmentSnapshotRepository.save(createRoomEnvironmentSnapshot(3L, ZonedDateTime.now().plusMinutes(15)));
        roomEnvironmentSnapshotRepository.save(createRoomEnvironmentSnapshot(4L, ZonedDateTime.now()));

        List<RoomEnvironmentSnapshot> found = roomEnvironmentSnapshotRepository.findByRoomId(3L);

        assertThat(found).hasSize(2);
    }

    private RoomEnvironmentSnapshot createRoomEnvironmentSnapshot(Long roomId, ZonedDateTime windowStart) {
        return RoomEnvironmentSnapshot.builder().roomId(roomId).windowStart(windowStart).build();
    }
}