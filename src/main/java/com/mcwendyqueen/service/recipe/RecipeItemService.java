package com.mcwendyqueen.service.recipe;

import com.mcwendyqueen.model.recipe.RecipeItem;
import com.mcwendyqueen.model.recipe.RecipeItemRequestDTO;
import com.mcwendyqueen.model.recipe.RecipeItemResponseDTO;

import java.util.List;
import java.util.Optional;

public interface RecipeItemService {
    List<RecipeItem> getAllRecipeItems();

    RecipeItemResponseDTO hydrateRecipeItem(RecipeItem recipeItem);

    Optional<RecipeItem> getRecipeItemById(long recipeId);

    Optional<RecipeItem> getRecipeItemByName(String menuItemName, String condimentName);

    RecipeItem createRecipeItem(RecipeItemRequestDTO newRecipeItem);

    Optional<RecipeItem> deleteRecipeItem(long recipeId);

    Optional<RecipeItem> deleteRecipeItem(String menuItemName, String condimentName);

    RecipeItem deleteRecipeItem(RecipeItemRequestDTO recipeItem);
}
