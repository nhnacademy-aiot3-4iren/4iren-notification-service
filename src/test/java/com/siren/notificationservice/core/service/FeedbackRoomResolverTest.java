package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import com.siren.notificationservice.core.dto.response.RoomSubResponse;
import com.siren.notificationservice.core.service.cache.LastMentionedRoomService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class FeedbackRoomResolverTest {

    private final LastMentionedRoomService lastMentionedRoomService = mock(LastMentionedRoomService.class);
    private final FeedbackRoomResolver feedbackRoomResolver = new FeedbackRoomResolver(lastMentionedRoomService);

    private final List<RoomSubResponse> subscribedRooms = List.of(
            new RoomSubResponse(1L, "301호", true),
            new RoomSubResponse(2L, "302호", true)
    );

    @Test
    void resolveReturnsMentionedRoomAndRemembersIt() {
        when(lastMentionedRoomService.find(1L)).thenReturn(Optional.empty());

        Optional<Long> result = feedbackRoomResolver.resolve("301호", 1L, subscribedRooms);

        assertThat(result).contains(1L);
        verify(lastMentionedRoomService).save(1L, 1L);
    }

    @Test
    void resolveFallsBackToLastMentionedRoomWhenNothingMentioned() {
        when(lastMentionedRoomService.find(1L)).thenReturn(Optional.of(2L));

        Optional<Long> result = feedbackRoomResolver.resolve(null, 1L, subscribedRooms);

        assertThat(result).contains(2L);
    }

    @Test
    void resolveFallsBackToOnlySubscriptionWhenNoMentionAndNoCache() {
        when(lastMentionedRoomService.find(1L)).thenReturn(Optional.empty());
        List<RoomSubResponse> onlyOneRoom = List.of(subscribedRooms.get(0));

        Optional<Long> result = feedbackRoomResolver.resolve(null, 1L, onlyOneRoom);

        assertThat(result).contains(1L);
    }

    @Test
    void resolveReturnsEmptyWhenNothingCanBeDecided() {
        when(lastMentionedRoomService.find(1L)).thenReturn(Optional.empty());

        Optional<Long> result = feedbackRoomResolver.resolve(null, 1L, subscribedRooms);

        assertThat(result).isEmpty();
    }

    @Test
    void matchReplyReturnsMatchingRoomAndRemembersIt() {
        List<FeedbackExtractionCache.RoomCandidate> candidates = List.of(
                new FeedbackExtractionCache.RoomCandidate(1L, "301호"),
                new FeedbackExtractionCache.RoomCandidate(2L, "302호")
        );

        Optional<Long> result = feedbackRoomResolver.matchReply("302호", 1L, candidates);

        assertThat(result).contains(2L);
        verify(lastMentionedRoomService).save(1L, 2L);
    }

    @Test
    void matchReplyReturnsEmptyWhenReplyMatchesNoCandidate() {
        List<FeedbackExtractionCache.RoomCandidate> candidates = List.of(
                new FeedbackExtractionCache.RoomCandidate(1L, "301호")
        );

        Optional<Long> result = feedbackRoomResolver.matchReply("모르는방", 1L, candidates);

        assertThat(result).isEmpty();
        verify(lastMentionedRoomService, never()).save(anyLong(), anyLong());
    }
}
