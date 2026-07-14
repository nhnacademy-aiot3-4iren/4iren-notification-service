package com.siren.notificationservice.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "notification_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class NotificationUser {

    @Id
    private Long userId;

    @Column(name = "user_name", length = 50, nullable = false)
    private String userName; // 개인화 메시지용 ("oo님" 호칭)

    @Column(name = "is_active", nullable = false)
    private boolean active; // 탈퇴/휴면 유저 알림 대상 제외 게이트

    @Column(name = "synced_at", nullable = false)
    private ZonedDateTime syncedAt; // 마지막 동기화 시각 (staleness 디버깅용)

    /**
     * Account API 유저 동기화 이벤트를 반영한다.
     * 이벤트 시각이 마지막 동기화 시각보다 과거이거나 같으면 무시한다 (stale 이벤트 가드).
     *
     * @param userName      동기화된 유저 이름
     * @param active        동기화된 활성 상태
     * @param eventSyncedAt 이벤트에 기록된 동기화 시각
     */
    public void syncFrom(String userName, boolean active, ZonedDateTime eventSyncedAt) {
        if (syncedAt != null && !eventSyncedAt.isAfter(syncedAt)) {
            return; // stale 이벤트 무시
        }
        this.userName = userName;
        this.active = active;
        this.syncedAt = eventSyncedAt;
    }
}
