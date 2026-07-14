package com.mcwendyqueen.service.menuitem;

import java.util.Optional;

import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemDurations;
import com.mcwendyqueen.model.menuitem.MenuItemRepository;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuItemServiceImplTest {

    private MenuItemRepository menuItemRepository;

    private MenuItemDurations menuItemDurations;

    private MenuItemServiceImpl menuItemService;

    @BeforeEach
    void setUp() {
        this.menuItemRepository = Mockito.mock(MenuItemRepository.class);
        this.menuItemDurations = Mockito.mock(MenuItemDurations.class);

        when(menuItemRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(menuItemDurations.getMenuItemDuration(any(MenuItemServiceImpl.MenuItemEnum.class))).thenReturn(60L);

        this.menuItemService = new MenuItemServiceImpl(menuItemRepository, menuItemDurations);
        clearInvocations(menuItemRepository);
    }

    @Test
    void createMenuItem_happyPath_savesAndReturns() {
        MenuItemRequestDTO request = new MenuItemRequestDTO("fries");

        when(menuItemRepository.findByName("fries")).thenReturn(Optional.empty());
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem menuItem = invocation.getArgument(0);

            menuItem.setId(9L);

            return menuItem;
        });

        MenuItem created = menuItemService.createMenuItem(request);

        assertEquals(9L, created.getId());
        assertEquals("fries", created.getName());
        verify(menuItemRepository, times(1)).save(any(MenuItem.class));
    }

    @Test
    void getMenuItemById_happyPath_returnsMenuItem() {
        MenuItem existing = new MenuItem(3L, "fries");

        when(menuItemRepository.findById(3L)).thenReturn(Optional.of(existing));

        Optional<MenuItem> result = menuItemService.getMenuItemById(3L);

        assertTrue(result.isPresent());
        assertEquals("fries", result.get().getName());
    }

    @Test
    void deleteMenuItemByName_happyPath_deletesAndReturns() {
        MenuItem existing = new MenuItem(4L, "burger");

        when(menuItemRepository.findByName("burger")).thenReturn(Optional.of(existing));

        Optional<MenuItem> deleted = menuItemService.deleteMenuItem("burger");

        assertTrue(deleted.isPresent());
        assertEquals(4L, deleted.get().getId());
        verify(menuItemRepository, times(1)).deleteById(4L);
    }
}
