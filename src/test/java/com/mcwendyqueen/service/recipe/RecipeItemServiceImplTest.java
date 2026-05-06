package com.mcwendyqueen.service.recipe;

import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.recipe.RecipeItem;
import com.mcwendyqueen.model.recipe.RecipeItemRequestDTO;
import com.mcwendyqueen.model.recipe.RecipeRepository;
import com.mcwendyqueen.service.condiment.CondimentItemServiceImpl;
import com.mcwendyqueen.service.menuitem.MenuItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeItemServiceImplTest {
    private RecipeRepository recipeRepository;
    private CondimentItemServiceImpl condimentItemService;
    private MenuItemService menuItemService;
    private RecipeItemServiceImpl recipeItemService;

    @BeforeEach
    void setUp() {
        recipeRepository = Mockito.mock(RecipeRepository.class);
        condimentItemService = Mockito.mock(CondimentItemServiceImpl.class);
        menuItemService = Mockito.mock(MenuItemService.class);

        when(menuItemService.getMenuItemIdByName(anyString())).thenReturn(1L);
        when(condimentItemService.getCondimentItemIdByName(anyString())).thenReturn(2L);
        when(recipeRepository.findByMenuIdAndCondimentId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(recipeRepository.save(Mockito.any(RecipeItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(menuItemService.getMenuItemByName(anyString())).thenReturn(Optional.of(new MenuItem(1L, "cheeseburger")));
        when(condimentItemService.getCondimentByName(anyString())).thenReturn(Optional.of(new CondimentItem(2L, "ketchup")));

        recipeItemService = new RecipeItemServiceImpl(recipeRepository, condimentItemService, menuItemService);
        clearInvocations(recipeRepository, condimentItemService, menuItemService);
    }

    @Test
    void createRecipeItem_happyPath_savesAndReturns() {
        RecipeItemRequestDTO request = new RecipeItemRequestDTO("cheeseburger", "ketchup");
        when(menuItemService.getMenuItemIdByName("cheeseburger")).thenReturn(10L);
        when(condimentItemService.getCondimentItemIdByName("ketchup")).thenReturn(20L);
        when(recipeRepository.findByMenuIdAndCondimentId(10L, 20L)).thenReturn(Optional.empty());
        when(recipeRepository.save(Mockito.any(RecipeItem.class))).thenAnswer(invocation -> {
            RecipeItem recipeItem = invocation.getArgument(0);
            recipeItem.setId(99L);
            return recipeItem;
        });

        RecipeItem created = recipeItemService.createRecipeItem(request);

        assertEquals(99L, created.getId());
        assertEquals(10L, created.getMenuId());
        assertEquals(20L, created.getCondimentId());
    }

    @Test
    void getRecipeItemById_happyPath_returnsRecipe() {
        RecipeItem existing = new RecipeItem(7L, 1L, 2L);
        when(recipeRepository.findById(7L)).thenReturn(Optional.of(existing));

        Optional<RecipeItem> result = recipeItemService.getRecipeItemById(7L);

        assertTrue(result.isPresent());
        assertEquals(7L, result.get().getId());
    }

    @Test
    void deleteRecipeItemById_happyPath_deletesAndReturns() {
        RecipeItem existing = new RecipeItem(5L, 1L, 2L);
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(existing));

        Optional<RecipeItem> deleted = recipeItemService.deleteRecipeItem(5L);

        assertTrue(deleted.isPresent());
        assertEquals(5L, deleted.get().getId());
        verify(recipeRepository, times(1)).deleteById(5L);
    }
}
