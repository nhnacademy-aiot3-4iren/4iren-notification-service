package com.siren.notificationservice.core.repository;

import com.siren.notificationservice.core.entity.RoomSubscription;
import com.siren.notificationservice.core.entity.RoomSubscriptionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomSubscriptionRepository
        extends JpaRepository<RoomSubscription, RoomSubscriptionId>, RoomSubscriptionRepositoryCustom {
}
