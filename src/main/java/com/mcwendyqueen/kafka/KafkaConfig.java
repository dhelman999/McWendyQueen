package com.mcwendyqueen.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;

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

    // Replaced by OrderEventConsumer — kept here as reference only.
    // @KafkaListener(id = "listenerId", topics = "order_events_topic")
    // public void listen(String in) {
    //     System.out.println(in);
    // }
}
