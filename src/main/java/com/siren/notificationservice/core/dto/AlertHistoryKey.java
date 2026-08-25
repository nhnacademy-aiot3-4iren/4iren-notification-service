package com.siren.notificationservice.core.dto;

import com.siren.notificationservice.core.entity.domain.BotType;

public record AlertHistoryKey(
        Long userId,
        BotType botType
) {}