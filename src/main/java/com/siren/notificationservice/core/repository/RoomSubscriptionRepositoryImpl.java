package com.siren.notificationservice.core.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.siren.notificationservice.core.entity.QRoomSubscription;
import com.siren.notificationservice.core.entity.SubscriptionStatus;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class RoomSubscriptionRepositoryImpl implements RoomSubscriptionRepositoryCustom {

    private static final QRoomSubscription ROOM_SUBSCRIPTION = QRoomSubscription.roomSubscription;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Long> findApprovedUserIdsByRoomId(Long roomId) {
        return queryFactory
                .select(ROOM_SUBSCRIPTION.id.userId)
                .from(ROOM_SUBSCRIPTION)
                .where(
                        ROOM_SUBSCRIPTION.id.roomId.eq(roomId),
                        ROOM_SUBSCRIPTION.status.eq(SubscriptionStatus.APPROVED)
                )
                .fetch();
    }

    @Override
    public List<Long> findApprovedRoomIdsByUserId(Long userId) {
        return queryFactory
                .select(ROOM_SUBSCRIPTION.id.roomId)
                .from(ROOM_SUBSCRIPTION)
                .where(
                        ROOM_SUBSCRIPTION.id.userId.eq(userId),
                        ROOM_SUBSCRIPTION.status.eq(SubscriptionStatus.APPROVED)
                )
                .fetch();
    }
}
