package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.PendingUserReply;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.telegram.agent.FeedbackExtractionAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 피드백 속에서 강의실을 판단하는 로직
 */
@Service
@RequiredArgsConstructor
public class FeedbackRoomResolver {
    private final LastMentionedRoomService lastMentionedRoomService;

    public Optional<Long> resolve(String mentionedRoomName, Long userId,
                                  List<UserRoomSubResponse.RoomSubResponse> subscribedRooms){
        Optional<Long> mentioned = resolveMentionedRoom(mentionedRoomName, subscribedRooms);
        // 텍스트에 실제로 언급된 강의실만 "마지막 언급"으로 남긴다 (구독 1개뿐이라 자동 선택된 경우는 언급이 아니므로 제외)
        mentioned.ifPresent(roomId -> lastMentionedRoomService.save(userId, roomId));

        return mentioned
                .or(() -> lastMentionedRoomService.find(userId))
                .or(() -> resolveIfOnlySubscription(subscribedRooms));
    }

    public Optional<Long> matchReply(String reply,Long userId, List<PendingUserReply.RoomCandidate> candidates){
        String normalizedReply = normalize(reply);

        List<PendingUserReply.RoomCandidate> matched = candidates.stream()
                .filter(c -> normalize(c.roomName()).contains(normalizedReply) || normalizedReply.contains(normalize(c.roomName())))
                .toList();
        Optional<Long> result = matched.size() == 1 ? Optional.of(matched.get(0).roomId()) : Optional.empty();
        result.ifPresent(roomId -> lastMentionedRoomService.save(userId, roomId));

        return result;
    }

    /**
     * {@link FeedbackExtractionAgent}가 판단한 언급 강의실 이름을 roomId로 변환한다.
     * 스키마 enum이 이미 구독 목록 안에서만 고르도록 제약해뒀으므로 여기서는 단순 조회만 한다.
     */
    private Optional<Long> resolveMentionedRoom(String mentionedRoomName, List<UserRoomSubResponse.RoomSubResponse> rooms){
        if(mentionedRoomName == null){
            return Optional.empty();
        }
        return rooms.stream()
                .filter(r -> r.roomName().equals(mentionedRoomName))
                .map(UserRoomSubResponse.RoomSubResponse::roomId)
                .findFirst();
    }

    private Optional<Long> resolveIfOnlySubscription(List<UserRoomSubResponse.RoomSubResponse> rooms) {
        return rooms.size() == 1 ? Optional.of(rooms.get(0).roomId()) : Optional.empty();
    }

    private String normalize(String text) {
        return text.replaceAll("\\s+", "").toLowerCase();
    }

}
