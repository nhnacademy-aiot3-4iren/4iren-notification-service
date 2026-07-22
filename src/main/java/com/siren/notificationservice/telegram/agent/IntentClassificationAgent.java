package com.siren.notificationservice.telegram.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.telegram.dto.IntentClassificationResult;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.routing.handle.IntentRouteDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IntentClassificationAgent {
    private static final String SYSTEM_PROMPT = """
        너는 텔레그램 챗봇에 들어오는 사용자의 자유 텍스트 메시지를 세 가지 의도 중 하나로 분류하는 역할이다.

        [분류 기준]
        - FEEDBACK: 사용자가 지금 있거나 있었던 강의실의 환경(온도/습도/공기질 등)에 대해 본인이 느낀 바를 말하는 경우.
          예: "너무 더워요", "좀 습한 것 같아요", "3시쯤에 너무 추웠어요", "지금은 딱 좋아요"
        - QUESTION: 강의실 상태를 묻거나, 실내 환경/공간 이용과 관련된 질문을 하는 경우.
          예: "지금 몇 도야?", "공기질 어때?", "환기는 언제 하는 게 좋아?", "적정 습도가 몇이야?"
        - FALLBACK: 위 두 가지 어디에도 해당하지 않는 경우 (인사, 잡담, 의미를 알 수 없는 메시지, 봇 기능과 무관한 내용).
          예: "안녕", "ㅋㅋㅋ", "고마워요"

        [규칙]
        - 한 메시지에 환경에 대한 감상과 질문이 함께 있으면(예: "너무 더운데 지금 몇 도야?") QUESTION으로 분류한다.
          (사용자의 직접적인 질문에 답하는 것이 우선이다)
        - 판단이 애매하면 FALLBACK으로 분류한다.
        - intent 필드에는 FEEDBACK, QUESTION, FALLBACK 중 정확히 하나만 채운다.
        """;
    private final ObjectMapper objectMapper;
    private final GoogleGenAiChatOptions googleGenAiChatOptions;
    private final IntentRouteDispatcher intentRouteDispatcher;

    private final ChatClient chatClient;
    public IntentClassificationAgent(@Qualifier("geminiJsonChatClientBuilder") ChatClient.Builder chatClient,
                                     ObjectMapper objectMapper,
                                     IntentRouteDispatcher intentRouteDispatcher) {
        this.chatClient = chatClient.defaultSystem(SYSTEM_PROMPT).build();
        this.objectMapper = objectMapper;
        this.googleGenAiChatOptions = buildJsonOptions();
        this.intentRouteDispatcher = intentRouteDispatcher;
    }

    /**
     * 자유 텍스트 메시지의 의도를 분류하고, 해당하는 IntentRouteHandler로 위임한다.
     * 분류 자체가 실패하거나(LLM 호출 예외, 파싱 실패) 텍스트 추출이 안 되는 경우
     * 전부 FALLBACK으로 처리한다 — 이 메서드가 예외를 던지면 DLQ 없는 리스너 구조상
     * 무한 재큐잉으로 이어지므로, 여기서 반드시 흡수해야 한다.
     *
     * @param event 원본 텔레그램 인바운드 이벤트
     */
    public void classify(TelegramInboundEvent event, Long userId) {
        IntentType intentType = IntentType.FALLBACK;
        try{
            String userMessage = event.update().getMessage().getText();
            String json = chatClient.prompt()
                    .user(userMessage)
                    .options(googleGenAiChatOptions)
                    .call()
                    .content();
            log.debug("[IntentClassificationAgent] LLM 호출 결과 json: {}", json);
            intentType = objectMapper.readValue(json, IntentClassificationResult.class).intent();
        } catch (Exception e) {
            log.warn("[IntentClassificationAgent] 의도 분류 실패 -> FALLBACK 처리", e);
        }
        intentRouteDispatcher.dispatch(intentType, event, userId);
    }

    private GoogleGenAiChatOptions buildJsonOptions(){
        String schemaJson = """
            {
              "type": "OBJECT",
              "properties": {
                "intent": { "type": "STRING", "enum": ["FEEDBACK", "QUESTION", "FALLBACK"] }
              },
              "required": ["intent"]
            }
            """;
        return GoogleGenAiChatOptions.builder()
                .model("gemini-flash-latest")
                .responseMimeType("application/json")
                .responseSchema(schemaJson)
                .build();
    }
}
