package com.mcwendyqueen.service;

import com.mcwendyqueen.model.RecipeItem;
import com.mcwendyqueen.model.RecipeItemRequestDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecipeItemServiceImpl implements RecipeItemService {
    private final Map<Long, RecipeItem> recipeItemsMap;

    private long recipeItemId;

    public RecipeItemServiceImpl() {
        recipeItemsMap = new ConcurrentHashMap<>();
        recipeItemId = 1;
        addAllRecipeItems();
    }

    @Override
    public List<RecipeItem> getAllRecipeItems() {
        return new ArrayList<>(recipeItemsMap.values());
    }

    @Override
    public RecipeItem createRecipeItem(RecipeItemRequestDTO newRecipeItem) {
        RecipeItem createdRecipeItem = new RecipeItem(recipeItemId++, newRecipeItem.getMenuId(), newRecipeItem.getCondimentId());
        recipeItemsMap.put(recipeItemId, createdRecipeItem);

        return createdRecipeItem;
    }

    @Override
    public RecipeItem deleteRecipeItem(RecipeItemRequestDTO recipeItem) {
        Optional<RecipeItem> removedRecipeItem = recipeItemsMap.values().stream()
                .filter(item -> item.getMenuId() == recipeItem.getMenuId() && item.getCondimentId() == recipeItem.getCondimentId())
                .findFirst();

        RecipeItem removedItem = removedRecipeItem.orElse(null);

        if (removedItem != null) {
            recipeItemsMap.remove(recipeItemId);
        }

        return removedItem;
    }

    private void addAllRecipeItems() {
        RecipeItem newRecipeItem = new RecipeItem(recipeItemId++, 1L, 1L);
        recipeItemsMap.put(newRecipeItem.getId(), newRecipeItem);

        newRecipeItem = new RecipeItem(recipeItemId++, 1L, 2L);
        recipeItemsMap.put(newRecipeItem.getId(), newRecipeItem);

        newRecipeItem = new RecipeItem(recipeItemId++, 1L, 3L);
        recipeItemsMap.put(newRecipeItem.getId(), newRecipeItem);
    }
}
