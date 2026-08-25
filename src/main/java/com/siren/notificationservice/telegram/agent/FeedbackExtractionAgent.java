package com.siren.notificationservice.telegram.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.entity.domain.SensorType;
import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FeedbackExtractionAgent {

    private static final String SYSTEM_PROMPT = """
        너는 강의실 환경에 대한 사용자 피드백 원문에서 세 가지를 추출하는 역할이다.

        [축별 체감 점수 - sensorScores]
        - TEMPERATURE(온도): -2(매우 추움) ~ 2(매우 더움)
        - HUMIDITY(습도): -2(매우 건조) ~ 2(매우 습함)
        - AIR_QUALITY(공기질): -2(매우 답답함/탁함) ~ 2(매우 쾌적함)
        - 텍스트에 언급된 축만 포함한다. 언급 안 된 축은 결과에서 아예 뺀다(0점 아님, 항목 생략).
        - **환경을 직접 언급하지 않는 증상 표현만으로는 점수를 추론하지 않는다.**
          "졸려요", "피곤해요", "집중이 안 돼요" 같은 표현은 방 환경 때문일 수도 있지만 컨디션/수면 부족 등
          다른 원인일 수도 있어서, 그 자체만으로는 어떤 축도 점수화하지 않는다.
          예: "여기 오니까 좀 졸리네요" -> sensorScores 비움
              "공기가 답답해서 졸려요" -> AIR_QUALITY로 점수화 (환경을 직접 언급했으므로)

        [지연 여부 - isDelayed]
        - "아까", "그때", "집에 와서" 등 현재 시점이 아니라 과거 어느 순간을 회상하는 표현이 있으면 true.
        - 지금 느끼는 걸 바로 말하는 경우는 false.

        [체감 시각 - experiencedHour / experiencedMeridiem / experiencedMinute]
        - "2시쯤", "오후 3시에" 처럼 구체적인 시각을 언급했으면 세 필드로 나눠 추출한다:
          experiencedHour(1~12), experiencedMeridiem("AM" 또는 "PM"), experiencedMinute(0~59, 분 언급 없으면 0).
        - 오전/오후가 문맥상 명백하면(예: "아침에", "새벽에" -> AM) 추론해서 채운다.
        - 시각 언급이 없거나, 오전/오후를 도저히 판단할 수 없으면 세 필드 모두 null.
        - 24시간 변환("오후 2시"->14시 같은 산술)은 네가 하지 않는다 — 위 세 필드만 정확히 채우면 된다.

        [언급된 강의실 - mentionedRoomName]
        - 사용자 메시지에는 "[구독 중인 강의실 목록]"이 함께 주어진다.
        - 피드백 원문에 그 목록 중 하나가 언급되어 있으면 그 이름을 그대로 반환한다(목록에 없는 이름을 만들어내지 않는다).
        - 언급이 없으면 null.

        예시:
        - "너무 더워요" -> sensorScores: [{TEMPERATURE, 2}], isDelayed: false, experiencedHour: null, experiencedMeridiem: null, experiencedMinute: null, mentionedRoomName: null
        - "아까 오후 2시쯤에 301호가 좀 건조했어요" (목록에 "301호" 있음) -> sensorScores: [{HUMIDITY, -1}], isDelayed: true, experiencedHour: 2, experiencedMeridiem: "PM", experiencedMinute: 0, mentionedRoomName: "301호"
        - "그냥 좀 졸리네요" -> sensorScores: [], isDelayed: false, experiencedHour: null, experiencedMeridiem: null, experiencedMinute: null, mentionedRoomName: null
        """;

    // SensorType enum 자체는 호출마다 바뀌지 않으므로 클래스 로드 시 한 번만 계산해둔다.
    private static final String SENSOR_ENUM_VALUES = Arrays.stream(SensorType.values())
            .map(t -> "\"" + t.name() + "\"")
            .collect(Collectors.joining(", "));

    // %s 위치 인자 대신 이름 있는 변수를 써서, 나중에 텍스트 순서를 바꾸다가
    // 인자 순서가 어긋나 엉뚱한 값이 들어가는 실수를 방지한다.
    private static final PromptTemplate USER_MESSAGE_TEMPLATE = new PromptTemplate("""
            [구독 중인 강의실 목록]
            {roomNames}

            [피드백 원문]
            {rawText}
            """);

    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;

    public FeedbackExtractionAgent(@Qualifier("geminiJsonChatClientBuilder") ChatClient.Builder chatClientBuilder,
                                    ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.objectMapper = objectMapper;
    }

    /**
     * 피드백 원문에서 축별 점수/지연여부/체감시각/언급된 강의실을 한 번에 추출한다.
     * 강의실 이름 목록은 호출마다 달라지므로(유저별 구독 목록), 응답 스키마의 {@code mentionedRoomName}
     * enum도 매 호출 새로 만든다 — 목록에 없는 이름을 모델이 지어내지 못하게 제약한다.
     * 실패 시(LLM 호출 예외, 파싱 실패) 안전한 기본값(빈 점수, 지연 아님, 시각/강의실 없음)을 반환한다 —
     * 이 메서드가 예외를 던지면 DLQ 없는 리스너 구조상 무한 재큐잉으로 이어지므로 여기서 흡수한다.
     *
     * @param rawText 피드백 원문
     * @param subscribedRoomNames 이 유저가 구독 중인 강의실 이름 목록 (비어있지 않아야 함 — 호출 전 확인 필요)
     * @return 추출 결과 (실패 시 기본값)
     */
    public FeedbackExtractionResult extract(String rawText, List<String> subscribedRoomNames) {
        try {
            String userMessage = USER_MESSAGE_TEMPLATE.render(Map.of(
                    "roomNames", String.join(", ", subscribedRoomNames),
                    "rawText", rawText
            ));

            long timingStart = System.currentTimeMillis();
            String json = chatClient.prompt()
                    .user(userMessage)
                    .options(buildJsonOptions(subscribedRoomNames))
                    .call()
                    .content();
            log.info("[Timing] FeedbackExtraction LLM: {}ms", System.currentTimeMillis() - timingStart);
            return objectMapper.readValue(json, FeedbackExtractionResult.class);
        } catch (Exception e) {
            log.warn("[FeedbackExtractionAgent] 추출 실패 -> 기본값 처리 (rawText={})", rawText, e);
            return new FeedbackExtractionResult(List.of(), false, null, null, null, null);
        }
    }

    private GoogleGenAiChatOptions buildJsonOptions(List<String> subscribedRoomNames) {
        String roomEnumValues = subscribedRoomNames.stream()
                .map(this::toJsonStringLiteral)
                .collect(Collectors.joining(", "));

        String schemaJson = """
            {
              "type": "OBJECT",
              "properties": {
                "sensorScores": {
                  "type": "ARRAY",
                  "items": {
                    "type": "OBJECT",
                    "properties": {
                      "sensorType": { "type": "STRING", "enum": [%s] },
                      "score": { "type": "INTEGER" }
                    },
                    "required": ["sensorType", "score"]
                  }
                },
                "isDelayed": { "type": "BOOLEAN" },
                "experiencedHour": { "type": "INTEGER", "minimum": 1, "maximum": 12, "nullable": true },
                "experiencedMeridiem": { "type": "STRING", "enum": ["AM", "PM"], "nullable": true },
                "experiencedMinute": { "type": "INTEGER", "minimum": 0, "maximum": 59, "nullable": true },
                "mentionedRoomName": { "type": "STRING", "enum": [%s], "nullable": true }
              },
              "required": ["sensorScores", "isDelayed"]
            }
            """.formatted(SENSOR_ENUM_VALUES, roomEnumValues);

        return GoogleGenAiChatOptions.builder()
                .model("gemini-flash-latest")
                .responseMimeType("application/json")
                .responseSchema(schemaJson)
                .build();
    }

    /**
     * 강의실 이름(외부 데이터, Core API 유래)을 JSON 스키마 enum에 안전하게 끼워 넣기 위해
     * 따옴표/역슬래시 등을 이스케이프한 JSON 문자열 리터럴로 변환.
     */
    private String toJsonStringLiteral(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("강의실 이름을 JSON으로 직렬화할 수 없음: " + value, e);
        }
    }
}
