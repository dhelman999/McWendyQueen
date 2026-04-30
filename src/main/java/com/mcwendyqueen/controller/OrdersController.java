package com.mcwendyqueen.controller;

import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.model.order.OrderResponseDTO;
import com.mcwendyqueen.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.ORDER_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

@Slf4j
@RestController
@RequestMapping(API_BASE_PATH + V1_PATH)
public class OrdersController {
    private final OrderService orderService;

    public OrdersController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping(ORDER_PATH)
    @Operation(summary = "List all orders", description = "Returns all orders in the system.")
    @ApiResponse(responseCode = "200", description = "Orders returned successfully")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        List<Order> allOrders = orderService.getAllOrders();

        List<OrderResponseDTO> response = allOrders.stream()
                .map(ModelMapperUtils::GetOrderResponseDTO)
                .toList();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(ORDER_PATH)
    @Operation(summary = "Create a new order", description = "Creates and saves a new order.")
    @ApiResponse(responseCode = "200", description = "Created order returned successfully")
    public ResponseEntity<OrderResponseDTO> createOrder(OrderRequestDTO newOrder) {
        Order createdOrder = orderService.createOrder(newOrder);
        OrderResponseDTO orderResponse = ModelMapperUtils.GetOrderResponseDTO(createdOrder);

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @DeleteMapping(ORDER_PATH)
    @Operation(summary = "Deletes an order", description = "Deletes and returns the order.")
    @ApiResponse(responseCode = "200", description = "Deleted order returned successfully")
    public ResponseEntity<OrderResponseDTO> deleteOrder(OrderRequestDTO order) {
        Order deletedOrder = orderService.deleteOrder(order);
        OrderResponseDTO orderResponse = ModelMapperUtils.GetOrderResponseDTO(deletedOrder);

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }
}
