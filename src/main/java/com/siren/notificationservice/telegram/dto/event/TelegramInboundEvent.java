package com.siren.notificationservice.telegram.dto.event;

import com.siren.notificationservice.core.entity.BotType;
import org.telegram.telegrambots.meta.api.objects.Update;

public record TelegramInboundEvent(
        BotType botType,
        Update update
) {
}
