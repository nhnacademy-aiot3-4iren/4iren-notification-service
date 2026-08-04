package com.siren.notificationservice.core.controller.doc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "피드백 데이터 내보내기 API", description = "AI Learning 팀 전용 내부 배치 조회 API. 서비스 간 호출용이며 외부에 공개하지 않는다.")
public interface FeedbackExportControllerDoc {

    /**
     * AI Learning팀이 학습용으로 당겨가는(pull) 피드백 데이터 CSV 조회.
     */
    @Operation(
            summary = "피드백 학습 데이터 CSV 커서 조회",
            description = "feedback_log_id 기준 커서 방식으로 피드백 축별 점수/외부날씨/실내외 온습도차를 "
                    + "long(key-value) 형식 CSV로 내려준다. 축별 점수가 없고 외부날씨 스냅샷도 없는 "
                    + "feedback_log는 CSV에 row가 하나도 없을 수 있으므로, 다음 페이지 sinceId는 "
                    + "CSV 내용이 아니라 응답 헤더 X-Last-Feedback-Log-Id 값을 사용해야 한다."
    )
    @ApiResponse(responseCode = "200", description = "CSV 조회 성공 (row가 0건이어도 200, 헤더만 있는 CSV로 응답)")
    @GetMapping(value = "/internal/feedback-exports", produces = "text/csv")
    ResponseEntity<String> export(
            @Parameter(description = "이 id 이후의 피드백만 조회한다. 최초 호출은 0, 이후로는 직전 응답의 X-Last-Feedback-Log-Id 값을 그대로 사용한다.", required = true)
            @RequestParam(defaultValue = "0") Long sinceId,
            @Parameter(description = "한 번에 조회할 최대 feedback_log 건수")
            @RequestParam(defaultValue = "5000") int limit
    );
}
