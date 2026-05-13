package com.mcwendyqueen.service.order;

import com.mcwendyqueen.kafka.KafkaMessageProducer;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderEventDTO;
import com.mcwendyqueen.model.order.OrderEventLightPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@Slf4j
public class OrderScheduler {

    private final KafkaMessageProducer kafkaMessageProducer;

    private final OrderService orderService;

    @Autowired
    public OrderScheduler(KafkaMessageProducer kafkaMessageProducer, OrderService orderService) {
        this.kafkaMessageProducer = kafkaMessageProducer;
        this.orderService = orderService;
    }

    @KafkaListener(id = "ordersListener", topics = "#{@kafkaAppConfig.ordersTopic}")
    public void listen(OrderEventDTO orderEvent) {
        log.info("Order event received: {}", orderEvent);
        Object payload = orderEvent.getPayload();

        if(!(payload instanceof OrderEventLightPayload orderPayload)){
            log.error("Order event payload is not a legal type");
            throw new IllegalArgumentException("Order event payload is not a legal type");
        }

        Optional<Order> currentOrderOpt = orderService.getOrderById(orderPayload.getId());

        if(currentOrderOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        Order currentOrder = currentOrderOpt.get();

        log.info("Processing order: {}", currentOrder.getName());
    }
}
