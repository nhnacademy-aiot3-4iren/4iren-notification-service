package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
class OutsideWeatherSnapshotRepositoryTest {

    @Autowired
    private OutsideWeatherSnapshotRepository outsideWeatherSnapshotRepository;

    @Test
    void findsSnapshotByRegionAndWindowStart() {

        LocalDateTime windowStart = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        outsideWeatherSnapshotRepository.save(OutsideWeatherSnapshot.builder()
                .nx(60).ny(127).windowStart(windowStart).build());

        Optional<OutsideWeatherSnapshot> found = outsideWeatherSnapshotRepository
                .findByNxAndNyAndWindowStart(60, 127, windowStart);

        assertThat(found).isPresent();
    }

    @Test
    void findsNothingWhenRegionDoesNotMatch() {
        LocalDateTime windowStart = LocalDateTime.now();
        outsideWeatherSnapshotRepository.save(OutsideWeatherSnapshot.builder()
                .nx(60).ny(127).windowStart(windowStart).build());

        Optional<OutsideWeatherSnapshot> found = outsideWeatherSnapshotRepository
                .findByNxAndNyAndWindowStart(61, 127, windowStart);

        assertThat(found).isEmpty();
    }
}
