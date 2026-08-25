package com.siren.notificationservice.core.service;

import com.siren.notificationservice.core.dto.EnrichedFeedbackLog;
import com.siren.notificationservice.core.dto.response.FeedbackExportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * feedback-exports 조회(FeedbackExportQueryService) + CSV 변환(FeedbackCsvConverter)을
 * 조합하는 오케스트레이션만 담당한다. DB 접근은 FeedbackExportQueryService가 이미 자기
 * 트랜잭션 안에서 끝내고 반환하므로, 여기엔 @Transactional이 필요 없다.
 */
@Service
@RequiredArgsConstructor
public class FeedbackExportService {

    private final FeedbackExportQueryService feedbackExportQueryService;
    private final FeedbackCsvConverter feedbackCsvConverter;

    /**
     * sinceId 이후의 피드백을 최대 limit건 조회해서 CSV로 조립한다. 점수/스냅샷이 하나도
     * 없는 feedback_log는 CSV row가 0개일 수 있어서, 다음 페이지 커서로 쓸 lastFeedbackLogId
     * (이번에 실제로 조회한 마지막 id)를 CSV와 별도로 반환한다.
     */
    public FeedbackExportResult exportAsCsv(Long sinceId, int limit) {
        List<EnrichedFeedbackLog> logs = feedbackExportQueryService.fetch(sinceId, limit);
        String csv = feedbackCsvConverter.toCsv(logs);
        Long lastFeedbackLogId = logs.isEmpty()
                ? sinceId
                : logs.get(logs.size() - 1).feedbackLog().getFeedbackLogId();
        return new FeedbackExportResult(csv, lastFeedbackLogId);
    }
}
