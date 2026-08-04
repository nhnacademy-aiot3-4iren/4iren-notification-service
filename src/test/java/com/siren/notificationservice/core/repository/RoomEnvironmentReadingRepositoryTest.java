package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.domain.EnvironmentMetricType;
import com.siren.notificationservice.core.entity.domain.RoomEnvironmentReadingId;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        // room_environment_reading의 value 컬럼이 H2 예약어라 그냥 두면 SQL 파싱이 깨짐 -> 식별자 전체를 quote 처리
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
class RoomEnvironmentReadingRepositoryTest {

    @Autowired
    private RoomEnvironmentReadingRepository roomEnvironmentReadingRepository;

    @Autowired
    private RoomEnvironmentSnapshotRepository roomEnvironmentSnapshotRepository;

    @Test
    void findsReadingsBySnapshotId() {
        RoomEnvironmentSnapshot snapshot = roomEnvironmentSnapshotRepository.save(snapshot());
        roomEnvironmentReadingRepository.save(reading(snapshot, EnvironmentMetricType.TEMPERATURE, "24.5"));
        roomEnvironmentReadingRepository.save(reading(snapshot, EnvironmentMetricType.HUMIDITY, "55.0"));

        List<RoomEnvironmentReading> found = roomEnvironmentReadingRepository
                .findById_SnapshotId(snapshot.getSnapshotId());

        assertThat(found).hasSize(2);
    }

    @Test
    void findsReadingsBySnapshotIdsInBatch() {
        RoomEnvironmentSnapshot snapshot1 = roomEnvironmentSnapshotRepository.save(snapshot());
        RoomEnvironmentSnapshot snapshot2 = roomEnvironmentSnapshotRepository.save(snapshot());
        roomEnvironmentReadingRepository.save(reading(snapshot1, EnvironmentMetricType.TEMPERATURE, "24.5"));
        roomEnvironmentReadingRepository.save(reading(snapshot2, EnvironmentMetricType.CO2, "500"));

        List<RoomEnvironmentReading> found = roomEnvironmentReadingRepository
                .findById_SnapshotIdIn(List.of(snapshot1.getSnapshotId(), snapshot2.getSnapshotId()));

        assertThat(found).hasSize(2);
    }

    @Test
    void findsNothingWhenSnapshotHasNoReadings() {
        RoomEnvironmentSnapshot snapshot = roomEnvironmentSnapshotRepository.save(snapshot());

        List<RoomEnvironmentReading> found = roomEnvironmentReadingRepository
                .findById_SnapshotId(snapshot.getSnapshotId());

        assertThat(found).isEmpty();
    }

    private RoomEnvironmentSnapshot snapshot() {
        return RoomEnvironmentSnapshot.builder().roomId(7L).windowStart(ZonedDateTime.now()).build();
    }

    private RoomEnvironmentReading reading(RoomEnvironmentSnapshot snapshot, EnvironmentMetricType metricType, String value) {
        return RoomEnvironmentReading.builder()
                .id(RoomEnvironmentReadingId.builder().metricType(metricType).build())
                .snapshot(snapshot)
                .value(new BigDecimal(value))
                .build();
    }
}
