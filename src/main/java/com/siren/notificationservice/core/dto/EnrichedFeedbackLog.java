package com.siren.notificationservice.core.dto;

import com.siren.notificationservice.core.entity.table.FeedbackLog;
import com.siren.notificationservice.core.entity.table.FeedbackScore;
import com.siren.notificationservice.core.entity.table.OutsideWeatherSnapshot;
import com.siren.notificationservice.core.entity.table.RoomEnvironmentReading;

import java.util.List;

/**
 * FeedbackExportQueryService가 배치 조회로 조립한, 피드백 한 건에 딸린 점수/실내값/외부날씨를
 * 미리 묶어둔 결과. FeedbackCsvConverter는 이 안의 데이터만으로 CSV를 만들고 DB에 다시 접근하지 않는다.
 */
public record EnrichedFeedbackLog(
        FeedbackLog feedbackLog,
        List<FeedbackScore> scores,
        List<RoomEnvironmentReading> readings,
        OutsideWeatherSnapshot outsideWeatherSnapshot
) {
}
