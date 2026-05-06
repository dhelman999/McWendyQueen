package com.mcwendyqueen.kafka;

import com.mcwendyqueen.model.event.BaseEventDTO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageProducer {

    private final KafkaTemplate<String, BaseEventDTO> kafkaTemplate;

    public KafkaMessageProducer(KafkaTemplate<String, BaseEventDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, BaseEventDTO baseEventDTO) {
        kafkaTemplate.send(topic, baseEventDTO);
    }
}
