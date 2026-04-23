package com.mcwendyqueen.service;

import com.mcwendyqueen.model.MenuItem;
import com.mcwendyqueen.model.MenuItemRequestDTO;

import java.util.List;

public interface MenuItemService {
    List<MenuItem> getAllMenuItems();

    MenuItem createMenuItem(MenuItemRequestDTO newMenuItem);

    MenuItem deleteMenuItem(MenuItemRequestDTO menuItem);
}
