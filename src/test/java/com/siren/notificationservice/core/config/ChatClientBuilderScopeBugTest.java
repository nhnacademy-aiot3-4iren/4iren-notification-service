package com.siren.notificationservice.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * geminiJsonChatClientBuilder가 @Scope("prototype")이 아니라 기본(싱글톤) 스코프였다면
 * 어떤 일이 벌어졌을지 재현.
 */
class ChatClientBuilderScopeBugTest {

    private final ChatClientConfig config = new ChatClientConfig();
    private final ChatModel chatModel = mock(ChatModel.class);
    private final ChatMemory chatMemory = mock(ChatMemory.class);

    @Test
    @DisplayName("싱글톤이면 Intent가 추가한 MessageChatMemoryAdvisor가 Feedback에도 적용됨")
    void Singleton() throws Exception {
        // 두 Agent가 config 메서드가 한 번만 만든 같은 Builder 인스턴스를 공유
        ChatClient.Builder sharedBuilder = config.geminiJsonChatClientBuilder(chatModel);

        // IntentClassificationAgent 생성자
        ChatClient intentAgentClient = sharedBuilder
                .defaultSystem("의도분류용 프롬프트")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // FeedbackExtractionAgent 생성자
        ChatClient feedbackAgentClient = sharedBuilder
                .defaultSystem("피드백추출용 프롬프트")
                .build();

        // geminiJsonChatClientBuilder 빈 자체가 SimpleLoggerAdvisor를 기본 포함하므로,
        // 정상이면 Feedback의 advisors는 SimpleLoggerAdvisor 1개뿐이어야 한다.
        assertThat(advisorsOf(feedbackAgentClient))
                .as("FeedbackExtractionAgent는 MessageChatMemoryAdvisor를 추가한 적이 없는데도 딸려옴")
                .hasSize(2)
                .anyMatch(a -> a instanceof MessageChatMemoryAdvisor);

        // 같은 Builder를 참조로 공유하므로 먼저 build()된 intentAgentClient도 결국 같은 상태를 본다.
        assertThat(advisorsOf(intentAgentClient)).isEqualTo(advisorsOf(feedbackAgentClient));
    }

    @Test
    @DisplayName("prototype 스코프면 Intent의 Advisor가 Feedback으로 새지 않음")
    void Prototype() throws Exception {
        // 빈 주입 지점마다 config 메서드가 다시 호출되어 새 Builder 인스턴스를 받음
        ChatClient.Builder intentBuilder = config.geminiJsonChatClientBuilder(chatModel);
        ChatClient.Builder feedbackBuilder = config.geminiJsonChatClientBuilder(chatModel);

        ChatClient intentAgentClient = intentBuilder
                .defaultSystem("의도분류용 프롬프트")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        ChatClient feedbackAgentClient = feedbackBuilder
                .defaultSystem("피드백추출용 프롬프트")
                .build();

        // 각자 새 Builder를 받아서 Feedback은 기본 SimpleLoggerAdvisor 1개뿐이고
        // Intent만 자기가 추가한 MessageChatMemoryAdvisor까지 2개를 가짐
        assertThat(advisorsOf(feedbackAgentClient))
                .hasSize(1)
                .noneMatch(a -> a instanceof MessageChatMemoryAdvisor);
        assertThat(advisorsOf(intentAgentClient))
                .hasSize(2)
                .anyMatch(a -> a instanceof MessageChatMemoryAdvisor);
    }

    // ChatClient가 등록된 advisor 목록을 밖으로 노출하는 API가 없어 리플렉션으로 내부 상태를 직접 읽음
    @SuppressWarnings("unchecked")
    private List<Advisor> advisorsOf(ChatClient chatClient) throws Exception {
        Field requestField = chatClient.getClass().getDeclaredField("defaultChatClientRequest");
        requestField.setAccessible(true);
        Object requestSpec = requestField.get(chatClient);

        Field advisorsField = requestSpec.getClass().getDeclaredField("advisors");
        advisorsField.setAccessible(true);
        return (List<Advisor>) advisorsField.get(requestSpec);
    }
}
