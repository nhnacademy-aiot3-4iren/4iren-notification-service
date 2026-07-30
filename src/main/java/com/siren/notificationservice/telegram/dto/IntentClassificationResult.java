package com.siren.notificationservice.telegram.dto;

import com.siren.notificationservice.telegram.routing.IntentType;

public record IntentClassificationResult (
        IntentType intent
){
}
