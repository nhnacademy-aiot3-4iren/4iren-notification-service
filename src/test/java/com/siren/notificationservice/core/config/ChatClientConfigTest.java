package com.siren.notificationservice.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ChatClientConfigTest {

    private final ChatClientConfig config = new ChatClientConfig();
    private final ChatModel chatModel = mock(ChatModel.class);

    @Test
    void geminiJsonChatClientBuilderIsCreated() {
        assertThat(config.geminiJsonChatClientBuilder(chatModel)).isNotNull();
    }

    @Test
    void chatMemoryIsCreated() {
        ChatMemoryRepository repository = mock(ChatMemoryRepository.class);

        ChatMemory chatMemory = config.chatMemory(repository);

        assertThat(chatMemory).isNotNull();
    }
}
