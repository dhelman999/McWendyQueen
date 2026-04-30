package com.mcwendyqueen.service.recipe;

import com.mcwendyqueen.model.recipe.RecipeItem;
import com.mcwendyqueen.model.recipe.RecipeItemRequestDTO;
import com.mcwendyqueen.model.recipe.RecipeItemResponseDTO;

import java.util.List;

public interface RecipeItemService {
    List<RecipeItem> getAllRecipeItems();

    RecipeItemResponseDTO hydrateRecipeItem(RecipeItem recipeItem);

    RecipeItem createRecipeItem(RecipeItemRequestDTO newRecipeItem);

    RecipeItem deleteRecipeItem(RecipeItemRequestDTO recipeItem);
}
