package com.siren.notificationservice.core.dto;

public record ConversationContext(
        String intentType,
        String lastQuestion,
        String lastAnswer
) {
}
