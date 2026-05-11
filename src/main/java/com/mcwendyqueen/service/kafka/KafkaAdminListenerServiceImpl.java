package com.mcwendyqueen.service.kafka;

import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Service
public class KafkaAdminListenerServiceImpl implements KafkaAdminListenerService {

    private final KafkaListenerEndpointRegistry registry;

    public KafkaAdminListenerServiceImpl(KafkaListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Collection<MessageListenerContainer> getAllListeners() {
        return registry.getAllListenerContainers();
    }

    @Override
    public Optional<MessageListenerContainer> pauseListener(String listenerId) {
        MessageListenerContainer listener = registry.getListenerContainer(listenerId);

        if(listener == null){
            return Optional.empty();
        }

        listener.pause();

        return Optional.of(listener);
    }

    @Override
    public Optional<MessageListenerContainer> resumeListener(String listenerId) {
        MessageListenerContainer listener = registry.getListenerContainer(listenerId);

        if(listener == null){
            return Optional.empty();
        }

        listener.resume();

        return Optional.of(listener);
    }
}
