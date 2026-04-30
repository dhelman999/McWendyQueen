package com.mcwendyqueen.service.menuitem;

import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemRepository;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.mcwendyqueen.service.menuitem.MenuItemServiceImpl.MenuItemEnum.CHEESEBURGER;
import static com.mcwendyqueen.service.menuitem.MenuItemServiceImpl.MenuItemEnum.FRIES;
import static com.mcwendyqueen.service.menuitem.MenuItemServiceImpl.MenuItemEnum.SALAD;

@Service
public class MenuItemServiceImpl implements MenuItemService {
    private final MenuItemRepository menuItemRepository;

    public static final long UNKNOWN_MENU_ITEM = -1;

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
            return shortName;
        }
    }

    @Autowired
    public MenuItemServiceImpl(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
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
        return createMenuItem(newMenuItem.getName());
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
    public long getMenuItemIdByName(String name) {
        Optional<MenuItem> menuItem = menuItemRepository.findByName(name);

        return menuItem.map(MenuItem::getId).orElse(UNKNOWN_MENU_ITEM);

    }

    public MenuItem createMenuItem(String name) {
        Optional<MenuItem> existingMenuItem = menuItemRepository.findByName(name);

        if(existingMenuItem.isPresent()) {
            // need to throw some problem or log
            return existingMenuItem.get();
        }

        MenuItem newMenuItem = new MenuItem(name);
        menuItemRepository.save(newMenuItem);

        return newMenuItem;
    }

    private void addAllMenuItems() {
        createMenuItem(CHEESEBURGER.getShortName());
        createMenuItem(FRIES.getShortName());
        createMenuItem(SALAD.getShortName());
    }
}
