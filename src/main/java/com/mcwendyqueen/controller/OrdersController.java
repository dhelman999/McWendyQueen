package com.mcwendyqueen.controller;

import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.model.order.OrderResponseDTO;
import com.mcwendyqueen.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.CONDIMENTS_PATH;
import static com.mcwendyqueen.ApiConstants.MENU_ITEM_PATH;
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
    @ApiResponse(responseCode = "201", description = "Created order returned successfully")
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody OrderRequestDTO newOrder) {
        Order createdOrder = orderService.createOrder(newOrder);
        OrderResponseDTO orderResponse = ModelMapperUtils.GetOrderResponseDTO(createdOrder);

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @DeleteMapping(ORDER_PATH)
    @Operation(summary = "Deletes an order", description = "Deletes and returns the order.")
    @ApiResponse(responseCode = "200", description = "Deleted order returned successfully")
    public ResponseEntity<OrderResponseDTO> deleteOrder(@Valid @RequestBody OrderRequestDTO order) {
        Order deletedOrder = orderService.deleteOrder(order);
        OrderResponseDTO orderResponse = ModelMapperUtils.GetOrderResponseDTO(deletedOrder);

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @PatchMapping(ORDER_PATH + CONDIMENTS_PATH + "/{orderId}")
    @Operation(summary = "Adds a condiment to an order", description = "Adds a condiment and returns an order.")
    @ApiResponse(responseCode = "200", description = "Added condiment to order successfully.")
    public ResponseEntity<OrderResponseDTO> addCondimentToOrder(@PathVariable Long orderId,
                                                                @Valid @RequestBody CondimentItemRequestDTO newCondiment) {
        Optional<Order> existingOrder = orderService.addCondimentToOrder(orderId, newCondiment);
        OrderResponseDTO orderResponse = null;

        if(existingOrder.isPresent()) {
            orderResponse = ModelMapperUtils.GetOrderResponseDTO(existingOrder.get());
        }

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @PatchMapping(ORDER_PATH + CONDIMENTS_PATH + "/name/{orderName}")
    @Operation(summary = "Adds a condiment to an order by name", description = "Adds a condiment and returns an order looked up by order name.")
    @ApiResponse(responseCode = "200", description = "Added condiment to order successfully.")
    public ResponseEntity<OrderResponseDTO> addCondimentToOrderByName(@PathVariable String orderName,
                                                                      @Valid @RequestBody CondimentItemRequestDTO newCondiment) {
        Optional<Order> existingOrder = orderService.addCondimentToOrder(orderName, newCondiment);
        OrderResponseDTO orderResponse = null;

        if(existingOrder.isPresent()) {
            orderResponse = ModelMapperUtils.GetOrderResponseDTO(existingOrder.get());
        }

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @DeleteMapping(ORDER_PATH + CONDIMENTS_PATH + "/{orderId}")
    @Operation(summary = "Deletes a condiment from an order", description = "Deletes a condiment and returns an order.")
    @ApiResponse(responseCode = "200", description = "Deleted condiment from order successfully.")
    public ResponseEntity<OrderResponseDTO> deleteCondimentFromOrder(@PathVariable Long orderId,
                                                                     @Valid @RequestBody CondimentItemRequestDTO newCondiment) {
        Optional<Order> existingOrder = orderService.removeCondimentFromOrder(orderId, newCondiment);
        OrderResponseDTO orderResponse = null;

        if(existingOrder.isPresent()) {
            orderResponse = ModelMapperUtils.GetOrderResponseDTO(existingOrder.get());
        }

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @DeleteMapping(ORDER_PATH + CONDIMENTS_PATH + "/name/{orderName}")
    @Operation(summary = "Deletes a condiment from an order by name", description = "Deletes a condiment and returns an order looked up by order name.")
    @ApiResponse(responseCode = "200", description = "Deleted condiment from order successfully.")
    public ResponseEntity<OrderResponseDTO> deleteCondimentFromOrderByName(@PathVariable String orderName,
                                                                           @Valid @RequestBody CondimentItemRequestDTO newCondiment) {
        Optional<Order> existingOrder = orderService.removeCondimentFromOrder(orderName, newCondiment);
        OrderResponseDTO orderResponse = null;

        if(existingOrder.isPresent()) {
            orderResponse = ModelMapperUtils.GetOrderResponseDTO(existingOrder.get());
        }

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @PatchMapping(ORDER_PATH + MENU_ITEM_PATH + "/{orderId}")
    @Operation(summary = "Adds a menu item to an order", description = "Adds a menu item and returns an order.")
    @ApiResponse(responseCode = "200", description = "Added condiment to order successfully.")
    public ResponseEntity<OrderResponseDTO> addMenuItemToOrder(@PathVariable Long orderId,
                                                               @Valid @RequestBody MenuItemRequestDTO newMenuItem) {
        Optional<Order> existingOrder = orderService.addMenuItemToOrder(orderId, newMenuItem);
        OrderResponseDTO orderResponse = null;

        if(existingOrder.isPresent()) {
            orderResponse = ModelMapperUtils.GetOrderResponseDTO(existingOrder.get());
        }

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @PatchMapping(ORDER_PATH + MENU_ITEM_PATH + "/name/{orderName}")
    @Operation(summary = "Adds a menu item to an order by name", description = "Adds a menu item and returns an order looked up by order name.")
    @ApiResponse(responseCode = "200", description = "Added menu item to order successfully.")
    public ResponseEntity<OrderResponseDTO> addMenuItemToOrderByName(@PathVariable String orderName,
                                                                     @Valid @RequestBody MenuItemRequestDTO newMenuItem) {
        Optional<Order> existingOrder = orderService.addMenuItemToOrder(orderName, newMenuItem);
        OrderResponseDTO orderResponse = null;

        if(existingOrder.isPresent()) {
            orderResponse = ModelMapperUtils.GetOrderResponseDTO(existingOrder.get());
        }

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @DeleteMapping(ORDER_PATH + MENU_ITEM_PATH + "/{orderId}")
    @Operation(summary = "Deletes a menu item from an order", description = "Deletes a menu item and returns an order.")
    @ApiResponse(responseCode = "200", description = "Deleted menu item from order successfully.")
    public ResponseEntity<OrderResponseDTO> deleteMenuItemFromOrder(@PathVariable Long orderId,
                                                                    @Valid @RequestBody MenuItemRequestDTO newMenuItem) {
        Optional<Order> existingOrder = orderService.removeMenuItemFromOrder(orderId, newMenuItem);
        OrderResponseDTO orderResponse = null;

        if(existingOrder.isPresent()) {
            orderResponse = ModelMapperUtils.GetOrderResponseDTO(existingOrder.get());
        }

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }

    @DeleteMapping(ORDER_PATH + MENU_ITEM_PATH + "/name/{orderName}")
    @Operation(summary = "Deletes a menu item from an order by name", description = "Deletes a menu item and returns an order looked up by order name.")
    @ApiResponse(responseCode = "200", description = "Deleted menu item from order successfully.")
    public ResponseEntity<OrderResponseDTO> deleteMenuItemFromOrderByName(@PathVariable String orderName,
                                                                          @Valid @RequestBody MenuItemRequestDTO newMenuItem) {
        Optional<Order> existingOrder = orderService.removeMenuItemFromOrder(orderName, newMenuItem);
        OrderResponseDTO orderResponse = null;

        if(existingOrder.isPresent()) {
            orderResponse = ModelMapperUtils.GetOrderResponseDTO(existingOrder.get());
        }

        return new ResponseEntity<>(orderResponse, HttpStatus.OK);
    }
}
