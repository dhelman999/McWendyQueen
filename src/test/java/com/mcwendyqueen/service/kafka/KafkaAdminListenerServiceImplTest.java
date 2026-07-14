package com.mcwendyqueen.service.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaAdminListenerServiceImplTest {
    private KafkaListenerEndpointRegistry registry;
    private KafkaAdminListenerServiceImpl kafkaAdminListenerService;

    @BeforeEach
    void setUp() {
        registry = Mockito.mock(KafkaListenerEndpointRegistry.class);
        kafkaAdminListenerService = new KafkaAdminListenerServiceImpl(registry);
    }

    @Test
    void pauseListener_happyPath_requestsPauseAndReturnsListener() {
        MessageListenerContainer listener = Mockito.mock(MessageListenerContainer.class);
        when(registry.getListenerContainer("ordersListener")).thenReturn(listener);

        Optional<MessageListenerContainer> result = kafkaAdminListenerService.pauseListener("ordersListener");

        assertTrue(result.isPresent());
        verify(listener, times(1)).pause();
    }

    @Test
    void resumeListener_happyPath_requestsResumeAndReturnsListener() {
        MessageListenerContainer listener = Mockito.mock(MessageListenerContainer.class);
        when(registry.getListenerContainer("ordersListener")).thenReturn(listener);

        Optional<MessageListenerContainer> result = kafkaAdminListenerService.resumeListener("ordersListener");

        assertTrue(result.isPresent());
        verify(listener, times(1)).resume();
    }

    @Test
    void pauseListener_missingListener_returnsEmpty() {
        when(registry.getListenerContainer("missingListener")).thenReturn(null);

        Optional<MessageListenerContainer> result = kafkaAdminListenerService.pauseListener("missingListener");

        assertTrue(result.isEmpty());
    }
}
