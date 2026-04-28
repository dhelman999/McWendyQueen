package com.mcwendyqueen.service;

import com.mcwendyqueen.model.CondimentItem;
import com.mcwendyqueen.model.MenuItem;
import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.RecipeItem;
import com.mcwendyqueen.model.RecipeItemRequestDTO;
import com.mcwendyqueen.model.RecipeItemResponseDTO;
import com.mcwendyqueen.model.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.CHEESE;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.LETTUCE;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.MUSTARD;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.ONIONS;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.PICKLES;
import static com.mcwendyqueen.service.CondimentItemServiceImpl.CondimentEnum.TOMATOES;
import static com.mcwendyqueen.service.MenuItemServiceImpl.MenuItemEnum.CHEESEBURGER;
import static com.mcwendyqueen.service.MenuItemServiceImpl.MenuItemEnum.SALAD;

@Service
public class RecipeItemServiceImpl implements RecipeItemService {
    private final RecipeRepository recipeRepository;

    private final CondimentItemServiceImpl condimentItemService;
    private final MenuItemService menuItemService;

    @Autowired
    public RecipeItemServiceImpl(
            RecipeRepository recipeRepository,
            CondimentItemServiceImpl condimentItemService,
            MenuItemService menuItemService) {
        this.recipeRepository = recipeRepository;
        this.condimentItemService = condimentItemService;
        this.menuItemService = menuItemService;
        addAllRecipeItems();
    }

    @Override
    public List<RecipeItem> getAllRecipeItems() {
        return recipeRepository.findAll();
    }

    @Override
    public RecipeItemResponseDTO hydrateRecipeItem(RecipeItem recipeItem) {
        if(recipeItem == null) {
            return null;
        }

        RecipeItemResponseDTO recipeDTO = ModelMapperUtils.GetRecipeItemResponseDTO(recipeItem);
        recipeDTO.setMenuItemName(menuItemService.getMenuItemById(recipeDTO.getMenuId()).map(MenuItem::getName).orElse(null));
        recipeDTO.setCondimentName(condimentItemService.getCondimentItemById(recipeDTO.getCondimentId()).map(CondimentItem::getName).orElse(null));

        return recipeDTO;
    }

    @Override
    public RecipeItem createRecipeItem(RecipeItemRequestDTO newRecipeItem) {
        return createRecipeItem(newRecipeItem.getMenuItemName(), newRecipeItem.getCondimentName());
    }

    @Override
    public RecipeItem deleteRecipeItem(RecipeItemRequestDTO recipeItem) {
        Optional<MenuItem> existingMenuItem = menuItemService.getMenuItemByName(recipeItem.getMenuItemName());
        Optional<CondimentItem> existingCondiment = condimentItemService.getCondimentByName(recipeItem.getCondimentName());

        if(existingMenuItem.isEmpty() || existingCondiment.isEmpty()) {
            return null;
        }

        Optional<RecipeItem> existingRecipe = recipeRepository.findByMenuIdAndCondimentId(
                existingMenuItem.get().getId(), existingCondiment.get().getId());

        if(existingRecipe.isEmpty()) {
            // todo throw an error and log
            return null;
        }

        recipeRepository.deleteById(existingRecipe.get().getId());

        return existingRecipe.get();
    }

    private RecipeItem createRecipeItem(String menuItemName, String condimentName) {
        long menuId = menuItemService.getMenuItemIdByName(menuItemName);
        long condimentId = condimentItemService.getCondimentItemIdByName(condimentName);
        Optional<RecipeItem> existingRecipe = recipeRepository.findByMenuIdAndCondimentId(menuId, condimentId);

        if(existingRecipe.isPresent()) {
            return existingRecipe.get();
        }

        RecipeItem newRecipeItem = new RecipeItem(menuId, condimentId);

        return recipeRepository.save(newRecipeItem);
    }

    private void addAllRecipeItems() {
        createRecipeItem(CHEESEBURGER.getShortName(), LETTUCE.getShortName());
        createRecipeItem(CHEESEBURGER.getShortName(), PICKLES.getShortName());
        createRecipeItem(CHEESEBURGER.getShortName(), TOMATOES.getShortName());
        createRecipeItem(CHEESEBURGER.getShortName(), CHEESE.getShortName());
        createRecipeItem(CHEESEBURGER.getShortName(), MUSTARD.getShortName());
        createRecipeItem(CHEESEBURGER.getShortName(), ONIONS.getShortName());

        // TODO - does this make sense?
        // createRecipeItem(FRIES.getShortName(), null);

        createRecipeItem(SALAD.getShortName(), LETTUCE.getShortName());
        createRecipeItem(SALAD.getShortName(), TOMATOES.getShortName());
        createRecipeItem(SALAD.getShortName(), CHEESE.getShortName());
        createRecipeItem(SALAD.getShortName(), ONIONS.getShortName());
    }
}
