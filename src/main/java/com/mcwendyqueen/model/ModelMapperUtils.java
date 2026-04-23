package com.mcwendyqueen.model;

import org.modelmapper.ModelMapper;

public class ModelMapperUtils {
    static ModelMapper modelMapper = new ModelMapper();

    public static CondimentItemResponseDTO GetCondimentItemResponseDTO(CondimentItem condimentItem) {
        return modelMapper.map(condimentItem, CondimentItemResponseDTO.class);
    }

    public static MenuItemResponseDTO GetMenuItemResponseDTO(MenuItem menuItem) {
        return modelMapper.map(menuItem, MenuItemResponseDTO.class);
    }

    public static RecipeItemResponseDTO GetRecipeItemResponseDTO(RecipeItem recipeItem) {
        return modelMapper.map(recipeItem, RecipeItemResponseDTO.class);
    }
}
