package com.mcwendyqueen.service.kafka;

import org.springframework.kafka.listener.MessageListenerContainer;

import java.util.Collection;
import java.util.Optional;

public interface KafkaAdminListenerService {

    Collection<MessageListenerContainer> getAllListeners();

    Optional<MessageListenerContainer> pauseListener(String listenerId);

    Optional<MessageListenerContainer> resumeListener(String listenerId);
}
