package com.mcwendyqueen.service;

import com.mcwendyqueen.model.MenuItem;
import com.mcwendyqueen.model.MenuItemRequestDTO;

import java.util.List;
import java.util.Optional;

public interface MenuItemService {
    List<MenuItem> getAllMenuItems();

    Optional<MenuItem> getMenuItemById(long menuItemId);

    Optional<MenuItem> getMenuItemByName(String menuItemName);

    long getMenuItemIdByName(String name);

    MenuItem createMenuItem(MenuItemRequestDTO newMenuItem);

    MenuItem deleteMenuItem(MenuItemRequestDTO menuItem);
}
