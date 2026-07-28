package com.siren.notificationservice.telegram.dto.feedback;

import com.siren.notificationservice.core.entity.domain.SensorType;

import java.util.List;

public record FeedbackExtractionResult(
        List<SensorScore> sensorScores,
        boolean isDelayed,
        // 체감 시각 - "오후 2시" 같은 자연어 해석까지는 LLM이 하고, 24시간제 변환(PM+12)은
        // 결정론적 산술이라 코드(FeedbackRouteHandler)에서 처리한다. 셋 중 하나라도 없으면(null) 시각 미상으로 취급.
        Integer experiencedHour,      // 1~12, 언급 없으면 null
        String experiencedMeridiem,   // "AM" | "PM", 언급 없으면 null
        Integer experiencedMinute,    // 0~59, 언급 없으면 null(분 언급 없이 시만 언급된 경우도 null -> 0으로 취급)
        String mentionedRoomName      // 원문에 언급된 구독 강의실 이름(주어진 목록 중 하나), 없으면 null
) {

    public record SensorScore(SensorType sensorType, Integer score) {}
}
