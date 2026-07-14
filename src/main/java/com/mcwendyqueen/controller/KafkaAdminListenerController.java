package com.mcwendyqueen.controller;

import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.kafka.MessageListenerResponseDTO;
import com.mcwendyqueen.service.kafka.KafkaAdminListenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mcwendyqueen.ApiConstants.ADMIN_PATH;
import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.KAFKA_PATH;
import static com.mcwendyqueen.ApiConstants.LISTENERS_PATH;
import static com.mcwendyqueen.ApiConstants.LIS_PAUSE_PATH;
import static com.mcwendyqueen.ApiConstants.LIS_RESUME_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;
import static com.mcwendyqueen.model.ModelMapperUtils.getMessageListenerResponseDTO;

@Slf4j
@RestController
@Validated
@RequestMapping(API_BASE_PATH + V1_PATH + ADMIN_PATH)
public class KafkaAdminListenerController {

    private final KafkaAdminListenerService kafkaAdminListenerService;

    public KafkaAdminListenerController(KafkaAdminListenerService kafkaAdminListenerService) {
        this.kafkaAdminListenerService = kafkaAdminListenerService;
    }

    @GetMapping(KAFKA_PATH + LISTENERS_PATH)
    @Operation(summary = "List all listeners", description = "Returns all listeners in the system.")
    @ApiResponse(responseCode = "200", description = "Kafka listeners returned successfully")
    public ResponseEntity<Collection<MessageListenerResponseDTO>> getAllListeners() {
        Collection<MessageListenerResponseDTO> listeners = kafkaAdminListenerService.getAllListeners()
                .stream()
                .map(ModelMapperUtils::getMessageListenerResponseDTO)
                .collect(Collectors.toList());

        return new ResponseEntity<>(listeners, HttpStatus.OK);
    }

    @PostMapping(KAFKA_PATH + LISTENERS_PATH + LIS_PAUSE_PATH + "/{listenerId}")
    @Operation(summary = "Pause specified listener", description = "Requests pause for the listener and returns current listener state. Pause is asynchronous, so pauseRequested may be true before containerPaused turns true.")
    @ApiResponse(responseCode = "200", description = "Kafka listener paused and returned successfully")
    public ResponseEntity<MessageListenerResponseDTO> pauseListener(@PathVariable @NotBlank(message = "listenerId is required") String listenerId) {
        Optional<MessageListenerContainer> listener = kafkaAdminListenerService.pauseListener(listenerId);

        if(listener.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listener not found");
        }

        MessageListenerResponseDTO response = getMessageListenerResponseDTO(listener.get());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(KAFKA_PATH + LISTENERS_PATH + LIS_RESUME_PATH + "/{listenerId}")
    @Operation(summary = "Resumes listener", description = "Requests resume for the listener and returns current listener state. Resume is asynchronous, so state may lag briefly before full processing restarts.")
    @ApiResponse(responseCode = "200", description = "Kafka listeners resumed and returned successfully")
    public ResponseEntity<MessageListenerResponseDTO> resumeListener(@PathVariable @NotBlank(message = "listenerId is required") String listenerId) {
        Optional<MessageListenerContainer> listener = kafkaAdminListenerService.resumeListener(listenerId);

        if(listener.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Listener not found");
        }

        MessageListenerResponseDTO response = getMessageListenerResponseDTO(listener.get());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
