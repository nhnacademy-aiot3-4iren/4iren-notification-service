package com.siren.notificationservice.core.dto.response;

/**
 * feedback-exports 응답 조립 결과. csv는 실제로 값이 있는 metric_type row만 담기 때문에
 * (점수/스냅샷이 하나도 없는 feedback_log는 row가 0개일 수 있음), 호출부가 다음 페이지
 * sinceId를 CSV 마지막 row가 아니라 lastFeedbackLogId(이번에 조회한 진짜 마지막 id)로
 * 갱신하도록 별도로 노출.
 */
public record FeedbackExportResult(String csv, Long lastFeedbackLogId) {
}
