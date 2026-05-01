package com.mcwendyqueen.service.order;

import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
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

    Optional<Order> addCondimentToOrder(Long orderId, CondimentItemRequestDTO newCondiment);

    Optional<Order> addCondimentToOrder(String orderName, CondimentItemRequestDTO newCondiment);

    Optional<Order> removeCondimentFromOrder(Long orderId, CondimentItemRequestDTO condimentToRemove);

    Optional<Order> removeCondimentFromOrder(String orderName, CondimentItemRequestDTO condimentToRemove);

    Optional<Order> addMenuItemToOrder(Long orderId, MenuItemRequestDTO newMenuItem);

    Optional<Order> addMenuItemToOrder(String orderName, MenuItemRequestDTO newMenuItem);

    Optional<Order> removeMenuItemFromOrder(Long orderId, MenuItemRequestDTO menuItemToRemove);

    Optional<Order> removeMenuItemFromOrder(String orderName, MenuItemRequestDTO menuItemToRemove);
}
