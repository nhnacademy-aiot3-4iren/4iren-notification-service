package com.siren.notificationservice.telegram.callback;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackActionTypeTest {

    @Test
    void fromPrefixFindsMatchingType() {
        Optional<CallbackActionType> result = CallbackActionType.fromPrefix("FB_ROOM");

        assertThat(result).contains(CallbackActionType.FEEDBACK_ROOM_SELECT);
    }

    @Test
    void fromPrefixReturnsEmptyWhenPrefixIsUnknown() {
        Optional<CallbackActionType> result = CallbackActionType.fromPrefix("모르는_접두사");

        assertThat(result).isEmpty();
    }
}
