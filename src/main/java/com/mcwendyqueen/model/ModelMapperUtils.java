package com.mcwendyqueen.model;

import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.condiment.CondimentItemRequestDTO;
import com.mcwendyqueen.model.condiment.CondimentItemResponseDTO;
import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.menuitem.MenuItemRequestDTO;
import com.mcwendyqueen.model.menuitem.MenuItemResponseDTO;
import com.mcwendyqueen.model.order.Order;
import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.model.order.OrderResponseDTO;
import com.mcwendyqueen.model.recipe.RecipeItem;
import com.mcwendyqueen.model.recipe.RecipeItemRequestDTO;
import com.mcwendyqueen.model.recipe.RecipeItemResponseDTO;
import org.modelmapper.ModelMapper;

public class ModelMapperUtils {
    static ModelMapper modelMapper = new ModelMapper();

    public static CondimentItem createCondimentItem(CondimentItemRequestDTO newCondiment) {
        return modelMapper.map(newCondiment, CondimentItem.class);
    }

    public static MenuItem createMenuItem(MenuItemRequestDTO newMenuItem) {
        return modelMapper.map(newMenuItem, MenuItem.class);
    }

    public static Order createOrder(OrderRequestDTO newOrder) {
        return modelMapper.map(newOrder, Order.class);
    }

    public static RecipeItem createRecipeItem(RecipeItemRequestDTO newRecipeItem) {
        return modelMapper.map(newRecipeItem, RecipeItem.class);
    }

    public static CondimentItemResponseDTO GetCondimentItemResponseDTO(CondimentItem condimentItem) {
        return modelMapper.map(condimentItem, CondimentItemResponseDTO.class);
    }

    public static MenuItemResponseDTO GetMenuItemResponseDTO(MenuItem menuItem) {
        return modelMapper.map(menuItem, MenuItemResponseDTO.class);
    }

    public static OrderResponseDTO GetOrderResponseDTO(Order order) {
        return modelMapper.map(order, OrderResponseDTO.class);
    }

    public static RecipeItemResponseDTO GetRecipeItemResponseDTO(RecipeItem recipeItem) {
        return modelMapper.map(recipeItem, RecipeItemResponseDTO.class);
    }
}
