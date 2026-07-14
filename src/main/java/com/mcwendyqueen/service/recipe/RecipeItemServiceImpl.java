package com.mcwendyqueen.service.recipe;

import java.util.List;
import java.util.Optional;

import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.recipe.RecipeItem;
import com.mcwendyqueen.model.recipe.RecipeItemRequestDTO;
import com.mcwendyqueen.model.recipe.RecipeItemResponseDTO;
import com.mcwendyqueen.model.recipe.RecipeRepository;
import com.mcwendyqueen.service.condiment.CondimentItemServiceImpl;
import com.mcwendyqueen.service.menuitem.MenuItemService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.CHEESE;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.LETTUCE;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.MUSTARD;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.ONIONS;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.PICKLES;
import static com.mcwendyqueen.service.condiment.CondimentItemServiceImpl.CondimentEnum.TOMATOES;
import static com.mcwendyqueen.service.menuitem.MenuItemServiceImpl.MenuItemEnum.CHEESEBURGER;
import static com.mcwendyqueen.service.menuitem.MenuItemServiceImpl.MenuItemEnum.SALAD;

@Service
public class RecipeItemServiceImpl implements RecipeItemService {

    private final RecipeRepository recipeRepository;

    private final CondimentItemServiceImpl condimentItemService;

    private final MenuItemService menuItemService;

    @Autowired
    public RecipeItemServiceImpl(RecipeRepository recipeRepository,
            CondimentItemServiceImpl condimentItemService, MenuItemService menuItemService) {
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
    public Optional<RecipeItem> getRecipeItemById(long recipeId) {
        return recipeRepository.findById(recipeId);
    }

    @Override
    public Optional<RecipeItem> getRecipeItemByName(String menuItemName, String condimentName) {
        long menuId = menuItemService.getMenuItemIdByName(menuItemName);
        long condimentId = condimentItemService.getCondimentItemIdByName(condimentName);

        return recipeRepository.findByMenuIdAndCondimentId(menuId, condimentId);
    }

    @Override
    public RecipeItemResponseDTO hydrateRecipeItem(RecipeItem recipeItem) {
        if (recipeItem == null) {
            return null;
        }

        RecipeItemResponseDTO recipeDTO = ModelMapperUtils.GetRecipeItemResponseDTO(recipeItem);

        recipeDTO.setMenuItemName(menuItemService.getMenuItemById(recipeDTO.getMenuId())
                .map(MenuItem::getName)
                .orElse(null));
        recipeDTO.setCondimentName(condimentItemService.getCondimentItemById(recipeDTO.getCondimentId())
                .map(CondimentItem::getName)
                .orElse(null));

        return recipeDTO;
    }

    @Override
    public RecipeItem createRecipeItem(RecipeItemRequestDTO newRecipeItem) {
        return createRecipeItem(newRecipeItem.getMenuItemName(), newRecipeItem.getCondimentName());
    }

    @Override
    public RecipeItem deleteRecipeItem(RecipeItemRequestDTO recipeItem) {
        Optional<MenuItem> existingMenuItem =
                menuItemService.getMenuItemByName(recipeItem.getMenuItemName());
        Optional<CondimentItem> existingCondiment =
                condimentItemService.getCondimentByName(recipeItem.getCondimentName());

        if (existingMenuItem.isEmpty() || existingCondiment.isEmpty()) {
            return null;
        }

        Optional<RecipeItem> existingRecipe = recipeRepository.findByMenuIdAndCondimentId(
                existingMenuItem.get().getId(), existingCondiment.get().getId());

        if (existingRecipe.isEmpty()) {
            // todo throw an error and log
            return null;
        }

        recipeRepository.deleteById(existingRecipe.get().getId());

        return existingRecipe.get();
    }

    @Override
    public Optional<RecipeItem> deleteRecipeItem(long recipeId) {
        Optional<RecipeItem> recipeToDelete = recipeRepository.findById(recipeId);

        if (recipeToDelete.isPresent()) {
            recipeRepository.deleteById(recipeId);
        }

        return recipeToDelete;
    }

    @Override
    public Optional<RecipeItem> deleteRecipeItem(String menuItemName, String condimentName) {
        Optional<RecipeItem> recipeToDelete = getRecipeItemByName(menuItemName, condimentName);

        if (recipeToDelete.isPresent()) {
            recipeRepository.deleteById(recipeToDelete.get().getId());
        }

        return recipeToDelete;
    }

    private RecipeItem createRecipeItem(String menuItemName, String condimentName) {
        long menuId = menuItemService.getMenuItemIdByName(menuItemName);
        long condimentId = condimentItemService.getCondimentItemIdByName(condimentName);
        Optional<RecipeItem> existingRecipe =
                recipeRepository.findByMenuIdAndCondimentId(menuId, condimentId);

        if (existingRecipe.isPresent()) {
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
