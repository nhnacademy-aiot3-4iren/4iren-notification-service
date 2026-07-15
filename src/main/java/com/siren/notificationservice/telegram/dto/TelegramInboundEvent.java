package com.siren.notificationservice.telegram.dto;

import com.siren.notificationservice.core.entity.BotType;
import org.telegram.telegrambots.meta.api.objects.Update;

public record TelegramInboundEvent(
        BotType botType,
        Update update
) {
}
