package com.mcwendyqueen.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
@ConfigurationProperties("kafka-topics.prod")
public class KafkaAppConfig {

    private boolean enabled;

    private String ordersTopic;
}
