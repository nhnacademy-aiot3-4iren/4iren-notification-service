package com.siren.notificationservice.telegram.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import com.siren.notificationservice.telegram.routing.IntentType;
import com.siren.notificationservice.telegram.routing.handler.IntentRouteDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntentClassificationAgentTest {

    private final ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
    private final ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IntentRouteDispatcher intentRouteDispatcher = mock(IntentRouteDispatcher.class);
    private final ChatMemory chatMemory = mock(ChatMemory.class);

    private IntentClassificationAgent buildAgent() {
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultAdvisors(any(Advisor.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        return new IntentClassificationAgent(chatClientBuilder, objectMapper, intentRouteDispatcher, chatMemory);
    }

    private TelegramInboundEvent textEvent(String text) {
        Chat chat = new Chat();
        chat.setId(100L);
        Message message = new Message();
        message.setChat(chat);
        message.setText(text);
        message.setDate((int) (System.currentTimeMillis() / 1000));
        Update update = new Update();
        update.setMessage(message);
        return new TelegramInboundEvent(BotType.USER_BOT, update);
    }

    @Test
    void classifyDispatchesParsedIntent() {
        when(callResponseSpec.content()).thenReturn("{\"intent\":\"FEEDBACK\"}");
        IntentClassificationAgent agent = buildAgent();
        TelegramInboundEvent event = textEvent("너무 더워요");

        agent.classify(event, 1L);

        verify(intentRouteDispatcher).dispatch(IntentType.FEEDBACK, event, 1L);
    }

    @Test
    void classifyDispatchesFallbackWhenLlmCallFails() {
        when(callResponseSpec.content()).thenThrow(new RuntimeException("LLM 호출 실패"));
        IntentClassificationAgent agent = buildAgent();
        TelegramInboundEvent event = textEvent("아무말");

        agent.classify(event, 1L);

        verify(intentRouteDispatcher).dispatch(IntentType.FALLBACK, event, 1L);
    }

    @Test
    void classifyDispatchesFallbackWhenJsonIsBroken() {
        when(callResponseSpec.content()).thenReturn("이건 json이 아님");
        IntentClassificationAgent agent = buildAgent();
        TelegramInboundEvent event = textEvent("아무말");

        agent.classify(event, 1L);

        verify(intentRouteDispatcher).dispatch(IntentType.FALLBACK, event, 1L);
    }
}
