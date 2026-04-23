package com.mcwendyqueen.service;

import com.mcwendyqueen.model.MenuItem;
import com.mcwendyqueen.model.MenuItemRequestDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MenuItemServiceImpl implements MenuItemService {
    private final Map<Long, MenuItem> menuItemsMap;

    private long menuItemId;

    public MenuItemServiceImpl() {
        menuItemsMap = new ConcurrentHashMap<>();
        menuItemId = 1;
        addAllMenuItems();
    }

    @Override
    public List<MenuItem> getAllMenuItems() {
        return new ArrayList<>(menuItemsMap.values());
    }

    @Override
    public MenuItem createMenuItem(MenuItemRequestDTO newMenuItem) {
        MenuItem createdMenuItem = new MenuItem(menuItemId++, newMenuItem.getName());
        menuItemsMap.put(menuItemId, createdMenuItem);

        return createdMenuItem;
    }

    @Override
    public MenuItem deleteMenuItem(MenuItemRequestDTO menuItem) {
        Optional<MenuItem> removedMenuItem = menuItemsMap.values().stream()
                .filter(item -> item.getName().equals(menuItem.getName()))
                .findFirst();

        MenuItem removedItem = removedMenuItem.orElse(null);

        if (removedItem != null) {
            menuItemsMap.remove(menuItemId);
        }

        return removedItem;
    }

    private void addAllMenuItems() {
        MenuItem newMenuItem = new MenuItem(menuItemId++, "cheeseburger");
        menuItemsMap.put(newMenuItem.getId(), newMenuItem);

        newMenuItem = new MenuItem(menuItemId++, "fries");
        menuItemsMap.put(newMenuItem.getId(), newMenuItem);

        newMenuItem = new MenuItem(menuItemId++, "salad");
        menuItemsMap.put(newMenuItem.getId(), newMenuItem);
    }
}
