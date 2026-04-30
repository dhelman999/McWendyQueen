package com.mcwendyqueen.controller;

import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;
import com.mcwendyqueen.model.menuitem.MenuItemResponseDTO;
import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.service.menuitem.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.MENU_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

@Slf4j
@RestController
@RequestMapping(API_BASE_PATH + V1_PATH)
public class MenuItemsController {
    private final MenuItemService menuItemService;

    public MenuItemsController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping(MENU_PATH)
    @Operation(summary = "List all menu items", description = "Returns all menu items in the system.")
    @ApiResponse(responseCode = "200", description = "Menu items returned successfully")
    public ResponseEntity<List<MenuItemResponseDTO>> getAllMenuItems() {
        List<MenuItem> allMenuItems = menuItemService.getAllMenuItems();

        List<MenuItemResponseDTO> response = allMenuItems.stream()
                .map(ModelMapperUtils::GetMenuItemResponseDTO)
                .toList();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(MENU_PATH)
    @Operation(summary = "Create a new menu item", description = "Creates and saves a new menu item.")
    @ApiResponse(responseCode = "200", description = "Created menu item returned successfully")
    public ResponseEntity<MenuItemResponseDTO> createMenuItem(MenuItemRequestDTO newMenuItem) {
        MenuItem createdMenuItem = menuItemService.createMenuItem(newMenuItem);
        MenuItemResponseDTO menuItemResponse = ModelMapperUtils.GetMenuItemResponseDTO(createdMenuItem);

        return new ResponseEntity<>(menuItemResponse, HttpStatus.OK);
    }

    @DeleteMapping(MENU_PATH)
    @Operation(summary = "Deletes a menu item", description = "Deletes and returns the menu item.")
    @ApiResponse(responseCode = "200", description = "Delete menu item returned successfully")
    public ResponseEntity<MenuItemResponseDTO> deleteMenuItem(MenuItemRequestDTO menuItem) {
        MenuItem deletedMenuItem = menuItemService.deleteMenuItem(menuItem);
        MenuItemResponseDTO menuItemResponse = ModelMapperUtils.GetMenuItemResponseDTO(deletedMenuItem);

        return new ResponseEntity<>(menuItemResponse, HttpStatus.OK);
    }
}
