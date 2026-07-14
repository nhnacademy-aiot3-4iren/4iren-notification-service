package com.siren.notificationservice.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "room_subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RoomSubscription {

    @EmbeddedId
    private RoomSubscriptionId id;

    /**
     * id.userId를 이 연관관계에서 파생시킨다 (FK 컬럼과 PK 컬럼이 user_id로 동일).
     * id와 notificationUser는 항상 같은 유저를 가리키도록 같이 세팅해야 한다.
     */
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private NotificationUser notificationUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt; // 이벤트 순서 역전 대비 (stale 이벤트 가드용)

    @Column(name = "alarm_enable", nullable = false)
    private boolean alarmEnable; // 구독은 승인됐지만 알림 수신만 개별로 끄고 켤 수 있음

    /**
     * Web이 발행한 구독 상태 스냅샷(상태 + 알림 수신 여부)을 반영한다.
     * 이벤트 시각이 마지막 갱신 시각보다 과거이거나 같으면 통째로 무시한다 (stale 이벤트 가드).
     * 두 필드를 별도 메서드로 나누면 같은 updatedAt을 공유하는 가드 특성상 두 번째 호출이
     * "이미 처리한 이벤트"로 오인되어 무시될 수 있어, 하나의 스냅샷으로 한 번에 반영한다.
     *
     * @param status         변경된 구독 상태
     * @param alarmEnable    변경된 알림 수신 여부
     * @param eventUpdatedAt 이벤트에 기록된 갱신 시각
     */
    public void syncFrom(SubscriptionStatus status, boolean alarmEnable, ZonedDateTime eventUpdatedAt) {
        if (updatedAt != null && !eventUpdatedAt.isAfter(updatedAt)) {
            return; // stale 이벤트 무시
        }
        this.status = status;
        this.alarmEnable = alarmEnable;
        this.updatedAt = eventUpdatedAt;
    }

    /**
     * 실제 알림 발송이 가능한 상태인지 판단한다 (구독 승인 + 알림 수신 둘 다 켜져 있어야 함).
     * REVOKED 상태에서도 alarmEnable 값 자체는 그대로 보존되므로(재승인 시 선호 유지),
     * "지금 보내도 되는가"는 저장 시점이 아니라 이 메서드로 판단한다.
     *
     * @return 구독 상태가 APPROVED이고 알림 수신이 켜져 있으면 true
     */
    public boolean isNotifiable() {
        return status == SubscriptionStatus.APPROVED && alarmEnable;
    }
}
