package com.siren.notificationservice.core.dto;

import org.springframework.ai.chat.messages.MessageType;

public record StoredMessage(
        MessageType type,
        String text
) {
}
