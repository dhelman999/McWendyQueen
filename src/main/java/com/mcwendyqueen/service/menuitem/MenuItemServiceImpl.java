package com.mcwendyqueen.service.menuitem;

import java.util.List;
import java.util.Optional;

import lombok.Getter;

import com.mcwendyqueen.model.menuitem.MenuItem;
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

    @Getter
    public enum MenuItemEnum {
        CHEESEBURGER("cheeseburger"),
        FRIES("fries"),
        SALAD("salad");

        private final String shortName;

        MenuItemEnum(String shortName) {
            this.shortName = shortName;
        }

        @Override
        public String toString() {
            return this.shortName;
        }
    }

    @Autowired
    public MenuItemServiceImpl(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
        addAllMenuItems();
    }

    @Override
    public List<MenuItem> getAllMenuItems() {
        return this.menuItemRepository.findAll();
    }

    @Override
    public Optional<MenuItem> getMenuItemById(long menuItemId) {
        return this.menuItemRepository.findById(menuItemId);
    }

    @Override
    public Optional<MenuItem> getMenuItemByName(String menuItemName) {
        return this.menuItemRepository.findByName(menuItemName);
    }

    @Override
    public MenuItem createMenuItem(MenuItemRequestDTO newMenuItem) {
        return createMenuItem(newMenuItem.getName());
    }

    @Override
    public MenuItem deleteMenuItem(MenuItemRequestDTO menuItem) {
        Long menuItemId = getMenuItemIdByName(menuItem.getName());
        Optional<MenuItem> menuItemToDelete = this.menuItemRepository.findById(menuItemId);
        MenuItem deletedMenuItem = null;

        if (menuItemToDelete.isPresent()) {
            this.menuItemRepository.deleteById(menuItemId);
            deletedMenuItem = menuItemToDelete.get();
        }

        return deletedMenuItem;
    }

    @Override
    public Optional<MenuItem> deleteMenuItem(long menuItemId) {
        Optional<MenuItem> menuItemToDelete = this.menuItemRepository.findById(menuItemId);

        if (menuItemToDelete.isPresent()) {
            this.menuItemRepository.deleteById(menuItemId);
        }

        return menuItemToDelete;
    }

    @Override
    public Optional<MenuItem> deleteMenuItem(String menuItemName) {
        Optional<MenuItem> menuItemToDelete = this.menuItemRepository.findByName(menuItemName);

        if (menuItemToDelete.isPresent()) {
            this.menuItemRepository.deleteById(menuItemToDelete.get().getId());
        }

        return menuItemToDelete;
    }

    @Override
    public long getMenuItemIdByName(String name) {
        Optional<MenuItem> menuItem = this.menuItemRepository.findByName(name);

        return menuItem.map(MenuItem::getId).orElse(UNKNOWN_MENU_ITEM);
    }

    public MenuItem createMenuItem(String name) {
        Optional<MenuItem> existingMenuItem = this.menuItemRepository.findByName(name);

        if (existingMenuItem.isPresent()) {
            // need to throw some problem or log
            return existingMenuItem.get();
        }

        MenuItem newMenuItem = new MenuItem(name);

        this.menuItemRepository.save(newMenuItem);

        return newMenuItem;
    }

    private void addAllMenuItems() {
        createMenuItem(CHEESEBURGER.getShortName());
        createMenuItem(FRIES.getShortName());
        createMenuItem(SALAD.getShortName());
    }
}
