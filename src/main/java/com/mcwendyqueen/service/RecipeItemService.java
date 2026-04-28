package com.mcwendyqueen.service;

import com.mcwendyqueen.model.RecipeItem;
import com.mcwendyqueen.model.RecipeItemRequestDTO;
import com.mcwendyqueen.model.RecipeItemResponseDTO;

import java.util.List;

public interface RecipeItemService {
    List<RecipeItem> getAllRecipeItems();

    RecipeItemResponseDTO hydrateRecipeItem(RecipeItem recipeItem);

    RecipeItem createRecipeItem(RecipeItemRequestDTO newRecipeItem);

    RecipeItem deleteRecipeItem(RecipeItemRequestDTO recipeItem);
}
