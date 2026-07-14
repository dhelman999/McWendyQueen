package com.mcwendyqueen.model.menuitem;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.mcwendyqueen.service.menuitem.MenuItemServiceImpl.MenuItemEnum;

@Component
public class MenuItemDurations {
    Map<MenuItemEnum, Long> menuItemDurationMap = new ConcurrentHashMap<>();

    MenuItemDurations() {
        menuItemDurationMap = new ConcurrentHashMap<>();

        loadMenuItemDurations();
    }

    private void loadMenuItemDurations() {
        menuItemDurationMap.put(MenuItemEnum.CHEESEBURGER, 5000L);
        menuItemDurationMap.put(MenuItemEnum.FRIES, 2500L);
        menuItemDurationMap.put(MenuItemEnum.SALAD, 3000L);
    }

    public Long getMenuItemDuration(MenuItemEnum menuItemEnum) {
        return menuItemDurationMap.get(menuItemEnum);
    }
}
