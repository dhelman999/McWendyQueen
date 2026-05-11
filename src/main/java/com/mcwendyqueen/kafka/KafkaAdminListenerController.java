package com.mcwendyqueen.kafka;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import static com.mcwendyqueen.ApiConstants.ADMIN_PATH;
import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.KAFKA_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

@Slf4j
@RestController
@Validated
@RequestMapping(API_BASE_PATH + V1_PATH + ADMIN_PATH)
public class KafkaAdminListenerController {

    private final KafkaListenerEndpointRegistry registry;

    public KafkaAdminListenerController(KafkaListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    @GetMapping(KAFKA_PATH)
    @Operation(summary = "List all listeners", description = "Returns all listeners in the system.")
    @ApiResponse(responseCode = "200", description = "Kafka listeners returned successfully")
    public ResponseEntity<Collection<MessageListenerContainer>> getAllListeners() {
        Collection<MessageListenerContainer> listeners = registry.getAllListenerContainers();

        return new ResponseEntity<>(listeners, HttpStatus.OK);
    }
}
