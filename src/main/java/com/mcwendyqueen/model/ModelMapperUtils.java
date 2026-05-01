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
import org.hibernate.Hibernate;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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
        OrderResponseDTO orderResponseDTO = new OrderResponseDTO();
        orderResponseDTO.setId(order.getId());
        orderResponseDTO.setName(order.getName());
        orderResponseDTO.setBaseMenuItem(order.getBaseMenuItem());

        Set<CondimentItemResponseDTO> condimentResponseSet;

        if (order.getCondiments() == null || !Hibernate.isInitialized(order.getCondiments())) {
            condimentResponseSet = Collections.emptySet();
        }
        else {
            condimentResponseSet = order.getCondiments().stream()
                    .map(ModelMapperUtils::GetCondimentItemResponseDTO)
                    .collect(Collectors.toSet());
        }

        orderResponseDTO.setCondiments(condimentResponseSet);

        return orderResponseDTO;
    }

    public static RecipeItemResponseDTO GetRecipeItemResponseDTO(RecipeItem recipeItem) {
        return modelMapper.map(recipeItem, RecipeItemResponseDTO.class);
    }
}
