package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.NotificationUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationUserRepository extends JpaRepository<NotificationUser, Long> {
}
