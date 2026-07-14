package com.siren.notificationservice.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "telegram_subscription",
uniqueConstraints = {
        @UniqueConstraint(name = "uq_telegram_subscription_user_bot_type", columnNames = {"user_id", "bot_type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TelegramSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_type", nullable = false)
    private BotType botType;

    @Column(name = "chat_id", length = 50, nullable = false)
    private String chatId;

    @Column(name = "is_active", nullable = false)
    private boolean active; // 봇 차단 시 false (my_chat_member 웹훅으로 갱신)

    @Column(name = "linked_at", nullable = false)
    private ZonedDateTime linkedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private NotificationUser notificationUser;

    /**
     * 봇 차단을 반영한다. {@code my_chat_member} 웹훅 이벤트 수신 시 호출된다.
     */
    public void block() {
        this.active = false;
    }

    /**
     * 봇 차단 해제를 반영한다. {@code my_chat_member} 웹훅 이벤트 수신 시 호출된다.
     */
    public void unblock() {
        this.active = true;
    }
}
