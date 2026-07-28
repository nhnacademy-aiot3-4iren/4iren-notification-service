package com.siren.notificationservice.telegram.dto.feedback;

import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZonedDateTime;

@Slf4j
public final class ExperiencedTimeResolver {

    private ExperiencedTimeResolver() {}

    /**
     * LLM이 추출한 체감 시각(12시간제 hour/meridiem/minute)을 24시간제로 조립해
     * 제출 시각의 날짜와 합친다. "오전/오후 -> +12" 같은 산술은 결정론적 절차라 여기서 코드로 처리한다
     * (LLM에게는 12시간제 해석까지만 맡김).
     * hour/meridiem 중 하나라도 없으면(언급 없음 or 오전/오후 판단 불가) 시각 미상으로 보고 null 반환.
     */
    public static ZonedDateTime resolve(FeedbackExtractionResult result, ZonedDateTime receivedAt){
        Integer hour12 = result.experiencedHour();
        String meridiem = result.experiencedMeridiem();
        if (hour12 == null || meridiem == null) {
            return null;
        }
        int minute = result.experiencedMinute() != null ? result.experiencedMinute() : 0;
        try {
            LocalTime time = LocalTime.of(to24Hour(hour12, meridiem), minute);
            return receivedAt.toLocalDate().atTime(time).atZone(receivedAt.getZone());
        } catch (DateTimeException e) {
            log.warn("[ExperiencedTimeResolver] 체감 시각 조립 실패, null로 처리 (hour={}, meridiem={}, minute={})", hour12, meridiem, minute, e);
            return null;
        }
    }

    private static int to24Hour(int hour12, String meridiem) {
        int hour = hour12 % 12; // 12 -> 0
        return "PM".equals(meridiem) ? hour + 12 : hour;
    }
}
