package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;
import com.siren.notificationservice.core.entity.domain.RoomEnvironmentReadingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomEnvironmentReadingRepository extends JpaRepository<RoomEnvironmentReading, RoomEnvironmentReadingId> {
}
