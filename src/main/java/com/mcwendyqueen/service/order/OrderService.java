package com.mcwendyqueen.service.order;

import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderRequestDTO;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    List<Order> getAllOrders();

    Optional<Order> getOrderById(long orderId);

    Optional<Order> getOrderByName(String orderName);

    long getOrderIdByName(String name);

    Order createOrder(OrderRequestDTO newOrder);

    Order deleteOrder(OrderRequestDTO order);

    Optional<Order> addMenuItem(long orderId, MenuItemRequestDTO menuItem);
}
