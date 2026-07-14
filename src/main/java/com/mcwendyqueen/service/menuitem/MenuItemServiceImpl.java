package com.mcwendyqueen.service.menuitem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;

import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemDuration;
import com.mcwendyqueen.model.menuitem.MenuItemDurations;
import com.mcwendyqueen.model.menuitem.MenuItemRepository;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.mcwendyqueen.service.menuitem.MenuItemServiceImpl.MenuItemEnum.CHEESEBURGER;
import static com.mcwendyqueen.service.menuitem.MenuItemServiceImpl.MenuItemEnum.FRIES;
import static com.mcwendyqueen.service.menuitem.MenuItemServiceImpl.MenuItemEnum.SALAD;

@Service
public class MenuItemServiceImpl implements MenuItemService {

    public static final long UNKNOWN_MENU_ITEM = -1;

    private final MenuItemRepository menuItemRepository;

    private final MenuItemDurations menuItemDurations;

    @Getter
    public enum MenuItemEnum {
        CHEESEBURGER("cheeseburger"),
        FRIES("fries"),
        SALAD("salad");

        private final String shortName;

        private static final Map<String, MenuItemEnum> BY_CODE = new HashMap<>();

        static {
            for (MenuItemEnum s : values()) {
                BY_CODE.put(s.shortName, s);
            }
        }

        MenuItemEnum(String shortName) {
            this.shortName = shortName;
        }

        @Override
        public String toString() {
            return this.shortName;
        }

        public static MenuItemEnum valueOfCode(String code) {
            return BY_CODE.get(code);
        }
    }

    @Autowired
    public MenuItemServiceImpl(MenuItemRepository menuItemRepository, MenuItemDurations menuItemDurations) {
        this.menuItemRepository = menuItemRepository;
        this.menuItemDurations = menuItemDurations;
        addAllMenuItems();
    }

    @Override
    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    @Override
    public Optional<MenuItem> getMenuItemById(long menuItemId) {
        return menuItemRepository.findById(menuItemId);
    }

    @Override
    public Optional<MenuItem> getMenuItemByName(String menuItemName) {
        return menuItemRepository.findByName(menuItemName);
    }

    @Override
    public MenuItem createMenuItem(MenuItemRequestDTO newMenuItem) {
        MenuItemEnum miEnum = MenuItemEnum.valueOfCode(newMenuItem.getName());

        if (miEnum == null) {
            throw new IllegalArgumentException("Invalid menu item name");
        }

        return createMenuItem(miEnum);
    }

    @Override
    public MenuItem deleteMenuItem(MenuItemRequestDTO menuItem) {
        Long menuItemId = getMenuItemIdByName(menuItem.getName());
        Optional<MenuItem> menuItemToDelete = menuItemRepository.findById(menuItemId);
        MenuItem deletedMenuItem = null;

        if (menuItemToDelete.isPresent()) {
            menuItemRepository.deleteById(menuItemId);
            deletedMenuItem = menuItemToDelete.get();
        }

        return deletedMenuItem;
    }

    @Override
    public Optional<MenuItem> deleteMenuItem(long menuItemId) {
        Optional<MenuItem> menuItemToDelete = menuItemRepository.findById(menuItemId);

        if (menuItemToDelete.isPresent()) {
            menuItemRepository.deleteById(menuItemId);
        }

        return menuItemToDelete;
    }

    @Override
    public Optional<MenuItem> deleteMenuItem(String menuItemName) {
        Optional<MenuItem> menuItemToDelete = menuItemRepository.findByName(menuItemName);

        if (menuItemToDelete.isPresent()) {
            menuItemRepository.deleteById(menuItemToDelete.get().getId());
        }

        return menuItemToDelete;
    }

    @Override
    public long getMenuItemIdByName(String name) {
        Optional<MenuItem> menuItem = menuItemRepository.findByName(name);

        return menuItem.map(MenuItem::getId).orElse(UNKNOWN_MENU_ITEM);
    }

    public MenuItem createMenuItem(MenuItemEnum menuItem) {
        Optional<MenuItem> existingMenuItem =
                menuItemRepository.findByName(menuItem.getShortName());

        if (existingMenuItem.isPresent()) {
            // need to throw some problem or log
            return existingMenuItem.get();
        }

        MenuItem newMenuItem = new MenuItem(menuItem.getShortName());
        long menuItemDuration = menuItemDurations.getMenuItemDuration(menuItem);
        MenuItemDuration newMenuItemDuration =
                new MenuItemDuration(newMenuItem.getId(), newMenuItem, menuItemDuration);

        // Set both parent and child relationship
        newMenuItem.setMiDuration(newMenuItemDuration);
        newMenuItemDuration.setMenuItem(newMenuItem);

        menuItemRepository.save(newMenuItem);

        return newMenuItem;
    }

    private void addAllMenuItems() {
        createMenuItem(CHEESEBURGER);
        createMenuItem(FRIES);
        createMenuItem(SALAD);
    }
}
