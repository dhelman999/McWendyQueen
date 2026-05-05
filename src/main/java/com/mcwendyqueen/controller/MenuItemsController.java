package com.mcwendyqueen.controller;

import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;
import com.mcwendyqueen.model.menuitem.MenuItemResponseDTO;
import com.mcwendyqueen.service.menuitem.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.MENU_PATH;
import static com.mcwendyqueen.ApiConstants.NAME_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

@Slf4j
@RestController
@Validated
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

    @GetMapping(MENU_PATH + "/{menuItemId}")
    @Operation(summary = "Get menu item by id", description = "Returns a single menu item by id.")
    @ApiResponse(responseCode = "200", description = "Menu item returned successfully")
    public ResponseEntity<MenuItemResponseDTO> getMenuItemById(@PathVariable @Positive(message = "menuItemId must be > 0") Long menuItemId) {
        Optional<MenuItem> existingMenuItem = menuItemService.getMenuItemById(menuItemId);

        if (existingMenuItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found");
        }

        MenuItemResponseDTO menuItemResponse = ModelMapperUtils.GetMenuItemResponseDTO(existingMenuItem.get());

        return new ResponseEntity<>(menuItemResponse, HttpStatus.OK);
    }

    @GetMapping(MENU_PATH + NAME_PATH + "/{menuItemName}")
    @Operation(summary = "Get menu item by name", description = "Returns a single menu item by name.")
    @ApiResponse(responseCode = "200", description = "Menu item returned successfully")
    public ResponseEntity<MenuItemResponseDTO> getMenuItemByName(@PathVariable @NotBlank(message = "menuItemName is required") String menuItemName) {
        Optional<MenuItem> existingMenuItem = menuItemService.getMenuItemByName(menuItemName);

        if (existingMenuItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found");
        }

        MenuItemResponseDTO menuItemResponse = ModelMapperUtils.GetMenuItemResponseDTO(existingMenuItem.get());

        return new ResponseEntity<>(menuItemResponse, HttpStatus.OK);
    }

    @PostMapping(MENU_PATH)
    @Operation(summary = "Create a new menu item", description = "Creates and saves a new menu item.")
    @ApiResponse(responseCode = "201", description = "Created menu item returned successfully")
    public ResponseEntity<MenuItemResponseDTO> createMenuItem(@Valid @RequestBody MenuItemRequestDTO newMenuItem) {
        MenuItem createdMenuItem = menuItemService.createMenuItem(newMenuItem);
        MenuItemResponseDTO menuItemResponse = ModelMapperUtils.GetMenuItemResponseDTO(createdMenuItem);

        return new ResponseEntity<>(menuItemResponse, HttpStatus.CREATED);
    }

    @DeleteMapping(MENU_PATH)
    @Operation(summary = "Deletes a menu item", description = "Deletes and returns the menu item.")
    @ApiResponse(responseCode = "200", description = "Delete menu item returned successfully")
    public ResponseEntity<MenuItemResponseDTO> deleteMenuItem(@Valid @RequestBody MenuItemRequestDTO menuItem) {
        MenuItem deletedMenuItem = menuItemService.deleteMenuItem(menuItem);

        if (deletedMenuItem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found");
        }

        MenuItemResponseDTO menuItemResponse = ModelMapperUtils.GetMenuItemResponseDTO(deletedMenuItem);

        return new ResponseEntity<>(menuItemResponse, HttpStatus.OK);
    }

    @DeleteMapping(MENU_PATH + "/{menuItemId}")
    @Operation(summary = "Deletes a menu item by id", description = "Deletes and returns the menu item.")
    @ApiResponse(responseCode = "200", description = "Delete menu item returned successfully")
    public ResponseEntity<MenuItemResponseDTO> deleteMenuItemById(@PathVariable @Positive(message = "menuItemId must be > 0") Long menuItemId) {
        Optional<MenuItem> deletedMenuItem = menuItemService.deleteMenuItem(menuItemId);

        if (deletedMenuItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found");
        }

        MenuItemResponseDTO menuItemResponse = ModelMapperUtils.GetMenuItemResponseDTO(deletedMenuItem.get());

        return new ResponseEntity<>(menuItemResponse, HttpStatus.OK);
    }

    @DeleteMapping(MENU_PATH + NAME_PATH + "/{menuItemName}")
    @Operation(summary = "Deletes a menu item by name", description = "Deletes and returns the menu item.")
    @ApiResponse(responseCode = "200", description = "Delete menu item returned successfully")
    public ResponseEntity<MenuItemResponseDTO> deleteMenuItemByName(@PathVariable @NotBlank(message = "menuItemName is required") String menuItemName) {
        Optional<MenuItem> deletedMenuItem = menuItemService.deleteMenuItem(menuItemName);

        if (deletedMenuItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found");
        }

        MenuItemResponseDTO menuItemResponse = ModelMapperUtils.GetMenuItemResponseDTO(deletedMenuItem.get());

        return new ResponseEntity<>(menuItemResponse, HttpStatus.OK);
    }
}
