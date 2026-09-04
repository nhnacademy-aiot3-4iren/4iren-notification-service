package com.siren.notificationservice.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * geminiJsonChatClientBuilder가 @Scope("prototype")이 아니라 기본(싱글톤) 스코프였다면
 * 어떤 일이 벌어졌을지 재현
 *
 Spring AI 1.1.8의 DefaultChatClientBuilder는 내부 스펙 객체(DefaultChatClientRequestSpec)를 필드 하나로 들고 있고
 defaultSystem()은 그 객체를 복사 없이 in-place로 mutate하며 build()도 그 객체를 참조 그대로 넘겨 ChatClient를 만든다.
 즉 여러 Agent가 같은 Builder를 공유하면 나중에 설정한 시스템 프롬프트가 먼저 build()된 ChatClient의 프롬프트까지 덮어쓴다.
 */
@DisplayName("Spring AI ChatClient.Builder Bean Scope 버그 재현")
class ChatClientBuilderScopeBugTest {

    private final ChatClientConfig config = new ChatClientConfig();
    private final ChatModel chatModel = mock(ChatModel.class);

    @Test
    @DisplayName("싱글톤이면 나중 Agent의 시스템 프롬프트가 먼저 Agent까지 덮어쓴다")
    void Singleton() throws Exception {

        //두 Agent가 config 메서드가 한 번만 만든 같은 Builder 인스턴스를 공유
        ChatClient.Builder sharedBuilder = config.geminiJsonChatClientBuilder(chatModel);

        ChatClient intentAgentClient = sharedBuilder.defaultSystem("의도분류용 프롬프트").build();

        ChatClient feedbackAgentClient = sharedBuilder.defaultSystem("피드백추출용 프롬프트").build();

        String intentActual = systemTextOf(intentAgentClient);

        assertThat(intentActual)
                .as("먼저 만들어진 의도분류 Agent가 나중 Agent의 프롬프트로 오염됨")
                .isEqualTo("피드백추출용 프롬프트");
        assertThat(systemTextOf(feedbackAgentClient)).isEqualTo("피드백추출용 프롬프트");
    }

    @Test
    @DisplayName("prototype 스코프면 Agent마다 시스템 프롬프트가 격리된다")
    void Prototype() throws Exception {

        // 주입 지점(Agent)마다 config 메서드가 다시 호출되어 새 Builder 인스턴스를 받음
        ChatClient.Builder intentBuilder = config.geminiJsonChatClientBuilder(chatModel);
        ChatClient.Builder feedbackBuilder = config.geminiJsonChatClientBuilder(chatModel);

        ChatClient intentAgentClient = intentBuilder.defaultSystem("의도분류용 프롬프트").build();
        ChatClient feedbackAgentClient = feedbackBuilder.defaultSystem("피드백추출용 프롬프트").build();

        String intentActual = systemTextOf(intentAgentClient);
        String feedbackActual = systemTextOf(feedbackAgentClient);

        assertThat(intentActual).isEqualTo("의도분류용 프롬프트");
        assertThat(feedbackActual).isEqualTo("피드백추출용 프롬프트");
    }

    // ChatClient가 시스템 프롬프트를 밖으로 노출하는 API가 없어 리플렉션으로 내부 상태를 직접 읽는다
    private String systemTextOf(ChatClient chatClient) throws Exception {
        Field requestField = chatClient.getClass().getDeclaredField("defaultChatClientRequest");
        requestField.setAccessible(true);
        Object requestSpec = requestField.get(chatClient);

        Field systemTextField = requestSpec.getClass().getDeclaredField("systemText");
        systemTextField.setAccessible(true);
        return (String) systemTextField.get(requestSpec);
    }
}
