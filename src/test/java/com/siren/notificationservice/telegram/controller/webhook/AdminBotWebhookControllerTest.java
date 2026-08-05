package com.siren.notificationservice.telegram.controller.webhook;

import com.siren.notificationservice.core.entity.domain.BotType;
import com.siren.notificationservice.telegram.dto.event.TelegramInboundEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminBotWebhookController.class)
@TestPropertySource(properties = {
        "rabbitmq.exchange.telegram-events=telegram.events",
        "rabbitmq.routing-key.telegram-inbound=telegram.inbound"
})
class AdminBotWebhookControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Test
    void webhookAdminPublishesEventAndReturnsOk() throws Exception {
        String body = """
                {"message":{"chat":{"id":100},"text":"/start","date":1690000000}}
                """;

        mockMvc.perform(post("/webhook/admin").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("telegram.events"), eq("telegram.inbound"), captor.capture());
        TelegramInboundEvent event = (TelegramInboundEvent) captor.getValue();
        assertThat(event.botType()).isEqualTo(BotType.ADMIN_BOT);
        assertThat(event.chatId()).isEqualTo("100");
    }
}
