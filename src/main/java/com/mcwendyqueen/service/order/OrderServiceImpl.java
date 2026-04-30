package com.mcwendyqueen.service.order;

import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentRepository;
import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderRepository;
import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.service.menuitem.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final MenuItemService menuItemService;
    private final CondimentRepository condimentRepository;
    public static final long UNKNOWN_ORDER = -1;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, MenuItemService menuItemService, CondimentRepository condimentRepository) {
        this.orderRepository = orderRepository;
        this.menuItemService = menuItemService;
        this.condimentRepository = condimentRepository;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Optional<Order> getOrderById(long orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public Optional<Order> getOrderByName(String orderName) {
        return orderRepository.findByName(orderName);
    }
    
    @Override
    public Order createOrder(OrderRequestDTO orderItem) {
        return createOrder(orderItem.getName());
    }

    @Override
    public Order deleteOrder(OrderRequestDTO order) {
        Long orderId = getOrderIdByName(order.getName());

        Optional<Order> orderToDelete = orderRepository.findById(orderId);
        Order deletedOrder = null;

        if (orderToDelete.isPresent()) {
            orderRepository.deleteById(orderId);
            deletedOrder = orderToDelete.get();
        }

        return deletedOrder;
    }

    @Override
    public Optional<Order> addMenuItem(long orderId, MenuItemRequestDTO menuItem) {
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<MenuItem> newMenuItem = menuItemService.getMenuItemByName(menuItem.getName());

        if(order.isEmpty() || newMenuItem.isEmpty()) {
            return order;
        }

        Order currentOrder = order.get();
        MenuItem currentMenuItem = newMenuItem.get();
        hydrateOrder(currentOrder, currentMenuItem);

        return Optional.of(currentOrder);
    }

    @Override
    public long getOrderIdByName(String name) {
        Optional<Order> order = orderRepository.findByName(name);

        return order.map(Order::getId).orElse(UNKNOWN_ORDER);
    }

    public Order createOrder(String name) {
        Optional<Order> existingOrder = orderRepository.findByName(name);

        if(existingOrder.isPresent()) {
            // need to throw some problem or log
            return existingOrder.get();
        }

        Order newOrder = new Order(name);
        orderRepository.save(newOrder);

        return newOrder;
    }

    private void hydrateOrder(Order currentOrder, MenuItem currentMenuItem) {
        Set<CondimentItem> orderCondiments = currentOrder.getCondiments();
        currentOrder.setBaseMenuItem(currentMenuItem);
        List<CondimentItem> condiments = condimentRepository.findAllCondimentItemsForMenuItem(currentMenuItem.getId());
        orderCondiments.addAll(condiments);
    }
}
