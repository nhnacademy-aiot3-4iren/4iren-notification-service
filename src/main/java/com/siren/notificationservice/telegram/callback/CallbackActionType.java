package com.siren.notificationservice.telegram.callback;

import java.util.Arrays;
import java.util.Optional;

public enum CallbackActionType {
    FEEDBACK_ROOM_SELECT("FB_ROOM"),
    QUESTION_CONTINUE("Q_REPLY");

    private final String prefix;
    CallbackActionType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() { return prefix; }
    public static Optional<CallbackActionType> fromPrefix(String prefix) {
        return Arrays.stream(values()).filter(t -> t.prefix.equals(prefix)).findFirst();
    }
}
