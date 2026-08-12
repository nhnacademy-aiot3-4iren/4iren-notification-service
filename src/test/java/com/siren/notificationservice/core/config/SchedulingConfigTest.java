package com.siren.notificationservice.core.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SchedulingConfigTest {

    @Test
    void lockProviderIsRedisBacked() {
        SchedulingConfig config = new SchedulingConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        LockProvider lockProvider = config.lockProvider(connectionFactory);

        assertThat(lockProvider).isInstanceOf(RedisLockProvider.class);
    }
}
