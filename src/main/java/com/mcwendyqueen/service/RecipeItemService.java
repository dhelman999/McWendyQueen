package com.mcwendyqueen.service;

import com.mcwendyqueen.model.RecipeItem;
import com.mcwendyqueen.model.RecipeItemRequestDTO;

import java.util.List;

public interface RecipeItemService {
    List<RecipeItem> getAllRecipeItems();

    RecipeItem createRecipeItem(RecipeItemRequestDTO newRecipeItem);

    RecipeItem deleteRecipeItem(RecipeItemRequestDTO recipeItem);
}
