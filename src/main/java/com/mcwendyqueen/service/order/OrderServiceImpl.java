package com.mcwendyqueen.service.order;

import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderRepository;
import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.service.condiment.CondimentItemService;
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
    private final CondimentItemService condimentItemService;
    public static final long UNKNOWN_ORDER = -1;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, MenuItemService menuItemService,
                            CondimentItemService condimentItemService) {
        this.orderRepository = orderRepository;
        this.menuItemService = menuItemService;
        this.condimentItemService = condimentItemService;
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
    public Optional<Order> addCondimentToOrder(Long orderId, CondimentItemRequestDTO newCondiment) {
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<CondimentItem> condiment = condimentItemService.getCondimentByName(newCondiment.getName());

        if(order.isEmpty() || condiment.isEmpty()) {
            return order;
        }

        if(order.get().getBaseMenuItem() == null) {
            return order;
        }

        orderRepository.save(hydrateOrder(order.get(), condiment.get()));

        return order;
    }

    @Override
    public Optional<Order> addCondimentToOrder(String orderName, CondimentItemRequestDTO newCondiment) {
        Optional<Order> order = orderRepository.findByName(orderName);
        Optional<CondimentItem> condiment = condimentItemService.getCondimentByName(newCondiment.getName());

        if(order.isEmpty() || condiment.isEmpty()) {
            return order;
        }

        if(order.get().getBaseMenuItem() == null) {
            return order;
        }

        orderRepository.save(hydrateOrder(order.get(), condiment.get()));

        return order;
    }

    @Override
    public Optional<Order> removeCondimentFromOrder(Long orderId, CondimentItemRequestDTO condimentToRemove) {
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<CondimentItem> condiment = condimentItemService.getCondimentByName(condimentToRemove.getName());

        if(order.isEmpty() || condiment.isEmpty()) {
            return order;
        }

        order.get().removeCondiment(condiment.get());

        return order;
    }

    @Override
    public Optional<Order> removeCondimentFromOrder(String orderName, CondimentItemRequestDTO condimentToRemove) {
        Optional<Order> order = orderRepository.findByName(orderName);
        Optional<CondimentItem> condiment = condimentItemService.getCondimentByName(condimentToRemove.getName());

        if(order.isEmpty() || condiment.isEmpty()) {
            return order;
        }

        order.get().removeCondiment(condiment.get());

        return order;
    }

    @Override
    public Optional<Order> addMenuItemToOrder(Long orderId, MenuItemRequestDTO newMenuItem) {
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<MenuItem> menuItem = menuItemService.getMenuItemByName(newMenuItem.getName());

        if(order.isEmpty() || menuItem.isEmpty()) {
            return order;
        }

        orderRepository.save(hydrateOrder(order.get(), menuItem.get()));

        return order;
    }

    @Override
    public Optional<Order> addMenuItemToOrder(String orderName, MenuItemRequestDTO newMenuItem) {
        Optional<Order> order = orderRepository.findByName(orderName);
        Optional<MenuItem> menuItem = menuItemService.getMenuItemByName(newMenuItem.getName());

        if(order.isEmpty() || menuItem.isEmpty()) {
            return order;
        }

        orderRepository.save(hydrateOrder(order.get(), menuItem.get()));

        return order;
    }

    @Override
    public Optional<Order> removeMenuItemFromOrder(Long orderId, MenuItemRequestDTO menuItemToRemove) {
        Optional<Order> order = orderRepository.findById(orderId);
        Optional<MenuItem> menuItem = menuItemService.getMenuItemByName(menuItemToRemove.getName());

        if(order.isEmpty() || menuItem.isEmpty()) {
            return order;
        }

        Order currentOrder = order.get();
        currentOrder.setBaseMenuItem(null);
        currentOrder.getCondiments().clear();

        orderRepository.save(currentOrder);

        return order;
    }

    @Override
    public Optional<Order> removeMenuItemFromOrder(String orderName, MenuItemRequestDTO menuItemToRemove) {
        Optional<Order> order = orderRepository.findByName(orderName);
        Optional<MenuItem> menuItem = menuItemService.getMenuItemByName(menuItemToRemove.getName());

        if(order.isEmpty() || menuItem.isEmpty()) {
            return order;
        }

        Order currentOrder = order.get();
        currentOrder.setBaseMenuItem(null);
        currentOrder.getCondiments().clear();

        orderRepository.save(currentOrder);

        return order;
    }

    @Override
    public long getOrderIdByName(String name) {
        Optional<Order> order = orderRepository.findByName(name);

        return order.map(Order::getId).orElse(UNKNOWN_ORDER);
    }

    private Order hydrateOrder(Order currentOrder, CondimentItem currentCondiment) {
        Set<CondimentItem> condiments = currentOrder.getCondiments();
        condiments.add(currentCondiment);

        return currentOrder;
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

    private Order hydrateOrder(Order currentOrder, MenuItem currentMenuItem) {
        Set<CondimentItem> orderCondiments = currentOrder.getCondiments();
        currentOrder.setBaseMenuItem(currentMenuItem);
        List<CondimentItem> condiments = condimentItemService.findAllCondimentItemsForMenuItem(currentMenuItem.getId());
        orderCondiments.addAll(condiments);

        return currentOrder;
    }
}
