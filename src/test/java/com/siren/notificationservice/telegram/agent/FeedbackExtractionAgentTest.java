package com.siren.notificationservice.telegram.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.telegram.dto.feedback.FeedbackExtractionResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedbackExtractionAgentTest {

    private final ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private FeedbackExtractionAgent buildAgent() {
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        return new FeedbackExtractionAgent(chatClientBuilder, objectMapper);
    }

    @Test
    void extractParsesLlmJsonIntoResult() {
        when(callResponseSpec.content()).thenReturn("""
                {"sensorScores":[{"sensorType":"TEMPERATURE","score":2}],"isDelayed":false,"experiencedHour":null,"experiencedMeridiem":null,"experiencedMinute":null,"mentionedRoomName":null}
                """);
        FeedbackExtractionAgent agent = buildAgent();

        FeedbackExtractionResult result = agent.extract("너무 더워요", List.of("301호"));

        assertThat(result.sensorScores()).hasSize(1);
        assertThat(result.sensorScores().get(0).score()).isEqualTo(2);
        assertThat(result.isDelayed()).isFalse();
    }

    @Test
    void extractReturnsEmptyDefaultWhenLlmCallFails() {
        when(callResponseSpec.content()).thenThrow(new RuntimeException("LLM 호출 실패"));
        FeedbackExtractionAgent agent = buildAgent();

        FeedbackExtractionResult result = agent.extract("너무 더워요", List.of("301호"));

        assertThat(result.sensorScores()).isEmpty();
        assertThat(result.isDelayed()).isFalse();
        assertThat(result.mentionedRoomName()).isNull();
    }

    @Test
    void extractReturnsEmptyDefaultWhenJsonIsBroken() {
        when(callResponseSpec.content()).thenReturn("이건 json이 아님");
        FeedbackExtractionAgent agent = buildAgent();

        FeedbackExtractionResult result = agent.extract("너무 더워요", List.of("301호"));

        assertThat(result.sensorScores()).isEmpty();
    }
}
