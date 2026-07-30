package com.siren.notificationservice.core.entity.table;

import com.siren.notificationservice.core.entity.domain.BotType;
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
    private Long telegramSubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_type", length = 20, nullable = false)
    private BotType botType;

    @Column(name = "chat_id", length = 50, nullable = false)
    private String chatId;

    @Column(name = "is_active", nullable = false)
    private boolean active; // 봇 차단 시 false (my_chat_member 웹훅으로 갱신)

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "user_id", nullable = false)
    private Long userId; // Account API 소유 유저 id (bare, 로컬 FK 없음)

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

    /**
     * 최초 연동시 chatId/createdAt 채워줌
     */
    public void link(String chatId, ZonedDateTime createdAt) {
        this.chatId = chatId;
        this.createdAt = createdAt;
    }
}
