package com.mcwendyqueen.kafka;

import java.util.HashMap;
import java.util.Map;

import com.mcwendyqueen.model.order.OrderEventDTO;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * TODO (MetLife/Smarsh interview prep):
 * 1) Wire this factory into a real @KafkaListener consumer.
 * 2) Set ENABLE_AUTO_COMMIT_CONFIG=false and ack manually after successful processing.
 * 3) Configure MAX_POLL_RECORDS and concurrency for throughput tuning.
 */
@Configuration
public class KafkaConsumerConfig {

    private final KafkaAppConfig kafkaAppConfig;

    public KafkaConsumerConfig(KafkaAppConfig kafkaAppConfig) {
        this.kafkaAppConfig = kafkaAppConfig;
    }

    @Bean
    public ConsumerFactory<String, OrderEventDTO> orderEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // TODO: read bootstrap servers from spring.kafka.bootstrap-servers
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "mcwendyqueen-order-consumers");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.mcwendyqueen.model.*");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderEventDTO> orderEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(orderEventConsumerFactory());
        // TODO: factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }
}
