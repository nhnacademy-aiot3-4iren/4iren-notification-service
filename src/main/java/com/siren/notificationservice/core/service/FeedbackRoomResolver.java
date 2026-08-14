package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.FeedbackExtractionCache;
import com.siren.notificationservice.core.dto.response.UserRoomSubResponse;
import com.siren.notificationservice.core.dto.response.RoomSubResponse;
import com.siren.notificationservice.core.service.cache.LastMentionedRoomService;
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
                                  List<RoomSubResponse> subscribedRooms){
        Optional<Long> mentioned = resolveMentionedRoom(mentionedRoomName, subscribedRooms);
        // 텍스트에 실제로 언급된 강의실만 "마지막 언급"으로 남긴다 (구독 1개뿐이라 자동 선택된 경우는 언급이 아니므로 제외)
        mentioned.ifPresent(roomId -> lastMentionedRoomService.save(userId, roomId));

        return mentioned // 자유 텍스트 속에서 LLM이 찾은 강의실이 있는경우 반환 없으면 아래로
                .or(() -> lastMentionedRoomService.find(userId)) // 자유텍스트속엔 없고 마지막으로 언급된 강의실이 남아있다면 반환 없으면 아래로
                .or(() -> resolveIfOnlySubscription(subscribedRooms)); // 강의실 구독을 하나만 했을경우 반환 그것도 없으면 empty
    }

    /**
     * 강의실 선택 콜백(버튼)의 답변을 후보와 매칭한다. callback_data가 항상 후보 이름 중
     * 하나를 그대로 담고 있어서(버튼을 그렇게 만들었으므로) 정확히 일치하는지만 보면 된다 —
     * 예전의 퍼지(부분 문자열) 매칭은 자유 텍스트 답변용이었는데, 이제 이 메서드는 콜백에서만
     * 호출되므로 더 이상 필요 없다.
     */
    public Optional<Long> matchReply(String reply, Long userId, List<FeedbackExtractionCache.RoomCandidate> candidates){
        Optional<Long> result = candidates.stream()
                .filter(c -> c.roomName().equals(reply))
                .map(FeedbackExtractionCache.RoomCandidate::roomId)
                .findFirst();
        result.ifPresent(roomId -> lastMentionedRoomService.save(userId, roomId));

        return result;
    }

    /**
     * FeedbackExtractionAgent가 판단한 언급 강의실 이름을 roomId로 변환한다.
     * 스키마 enum이 이미 구독 목록 안에서만 고르도록 제약해뒀으므로 여기서는 단순 조회만 한다.
     */
    private Optional<Long> resolveMentionedRoom(String mentionedRoomName, List<RoomSubResponse> rooms){
        if(mentionedRoomName == null){
            return Optional.empty();
        }
        return rooms.stream()
                .filter(r -> r.roomName().equals(mentionedRoomName))
                .map(RoomSubResponse::roomId)
                .findFirst();
    }

    private Optional<Long> resolveIfOnlySubscription(List<RoomSubResponse> rooms) {
        return rooms.size() == 1 ? Optional.of(rooms.get(0).roomId()) : Optional.empty();
    }

}
