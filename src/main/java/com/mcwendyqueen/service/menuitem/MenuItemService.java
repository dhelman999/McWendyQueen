package com.mcwendyqueen.service.menuitem;

import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;

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
