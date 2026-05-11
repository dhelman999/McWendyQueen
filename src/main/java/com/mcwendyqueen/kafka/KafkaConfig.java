package com.mcwendyqueen.kafka;

import com.mcwendyqueen.model.order.OrderEventDTO;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;

@EnableKafka
@Configuration
public class KafkaConfig {
    private final KafkaAppConfig kafkaAppConfig;

    public KafkaConfig(KafkaAppConfig kafkaAppConfig) {
        this.kafkaAppConfig = kafkaAppConfig;
    }

    @Bean
    public NewTopic orderEventTopic() {
        return TopicBuilder.name(kafkaAppConfig.getOrdersTopic())
                .partitions(2)
                .replicas(1)
                .build();
    }

    @KafkaListener(id = "listenerId", topics = "#{@kafkaAppConfig.ordersTopic}")
    public void listen(OrderEventDTO orderEvent) {

    }
}
