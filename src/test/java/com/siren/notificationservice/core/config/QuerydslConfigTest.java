package com.siren.notificationservice.core.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class QuerydslConfigTest {

    @Test
    void jpaQueryFactoryIsCreated() {
        QuerydslConfig config = new QuerydslConfig();
        EntityManager entityManager = mock(EntityManager.class);
        ReflectionTestUtils.setField(config, "entityManager", entityManager);

        assertThat(config.jpaQueryFactory()).isNotNull();
    }
}
