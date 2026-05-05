package com.mcwendyqueen.controller;

import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.recipe.RecipeItem;
import com.mcwendyqueen.model.recipe.RecipeItemRequestDTO;
import com.mcwendyqueen.model.recipe.RecipeItemResponseDTO;
import com.mcwendyqueen.service.recipe.RecipeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.NAME_PATH;
import static com.mcwendyqueen.ApiConstants.RECIPE_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

@Slf4j
@RestController
@Validated
@RequestMapping(API_BASE_PATH + V1_PATH)
public class RecipeItemsController {
    private final RecipeItemService recipeItemService;

    public RecipeItemsController(RecipeItemService recipeItemService) {
        this.recipeItemService = recipeItemService;
    }

    @GetMapping(RECIPE_PATH)
    @Operation(summary = "List all recipe items", description = "Returns all recipe items in the system.")
    @ApiResponse(responseCode = "200", description = "Recipe items returned successfully")
    public ResponseEntity<List<RecipeItemResponseDTO>> getAllRecipeItems() {
        List<RecipeItem> allRecipeItems = recipeItemService.getAllRecipeItems();

        List<RecipeItemResponseDTO> response = new ArrayList<>();
        allRecipeItems.forEach(item -> response.add(recipeItemService.hydrateRecipeItem(item)));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(RECIPE_PATH + "/{recipeId}")
    @Operation(summary = "Get recipe by id", description = "Returns a single recipe by id.")
    @ApiResponse(responseCode = "200", description = "Recipe returned successfully")
    public ResponseEntity<RecipeItemResponseDTO> getRecipeItemById(@PathVariable @Positive(message = "recipeId must be > 0") Long recipeId) {
        Optional<RecipeItem> existingRecipe = recipeItemService.getRecipeItemById(recipeId);

        if (existingRecipe.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
        }

        RecipeItemResponseDTO recipeResponse = recipeItemService.hydrateRecipeItem(existingRecipe.get());

        return new ResponseEntity<>(recipeResponse, HttpStatus.OK);
    }

    @GetMapping(RECIPE_PATH + NAME_PATH + "/{menuItemName}/{condimentName}")
    @Operation(summary = "Get recipe by names", description = "Returns a single recipe by menu item and condiment names.")
    @ApiResponse(responseCode = "200", description = "Recipe returned successfully")
    public ResponseEntity<RecipeItemResponseDTO> getRecipeItemByName(@PathVariable @NotBlank(message = "menuItemName is required") String menuItemName,
                                                                     @PathVariable @NotBlank(message = "condimentName is required") String condimentName) {
        Optional<RecipeItem> existingRecipe = recipeItemService.getRecipeItemByName(menuItemName, condimentName);

        if (existingRecipe.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
        }

        RecipeItemResponseDTO recipeResponse = recipeItemService.hydrateRecipeItem(existingRecipe.get());

        return new ResponseEntity<>(recipeResponse, HttpStatus.OK);
    }

    @PostMapping(RECIPE_PATH)
    @Operation(summary = "Create a new recipe item", description = "Creates and saves a new recipe item.")
    @ApiResponse(responseCode = "201", description = "Created recipe item returned successfully")
    public ResponseEntity<RecipeItemResponseDTO> createRecipeItem(@Valid @RequestBody RecipeItemRequestDTO newRecipeItem) {
        RecipeItem createdRecipeItem = recipeItemService.createRecipeItem(newRecipeItem);
        RecipeItemResponseDTO recipeItemResponse = ModelMapperUtils.GetRecipeItemResponseDTO(createdRecipeItem);

        return new ResponseEntity<>(recipeItemResponse, HttpStatus.CREATED);
    }

    @DeleteMapping(RECIPE_PATH)
    @Operation(summary = "Deletes a recipe item", description = "Deletes and returns the recipe item.")
    @ApiResponse(responseCode = "200", description = "Delete recipe item returned successfully")
    public ResponseEntity<RecipeItemResponseDTO> deleteRecipeItem(@Valid @RequestBody RecipeItemRequestDTO recipeItem) {
        RecipeItem deletedRecipeItem = recipeItemService.deleteRecipeItem(recipeItem);

        if (deletedRecipeItem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
        }

        RecipeItemResponseDTO recipeItemResponse = ModelMapperUtils.GetRecipeItemResponseDTO(deletedRecipeItem);

        return new ResponseEntity<>(recipeItemResponse, HttpStatus.OK);
    }

    @DeleteMapping(RECIPE_PATH + "/{recipeId}")
    @Operation(summary = "Deletes a recipe by id", description = "Deletes and returns the recipe item.")
    @ApiResponse(responseCode = "200", description = "Delete recipe item returned successfully")
    public ResponseEntity<RecipeItemResponseDTO> deleteRecipeItemById(@PathVariable @Positive(message = "recipeId must be > 0") Long recipeId) {
        Optional<RecipeItem> deletedRecipe = recipeItemService.deleteRecipeItem(recipeId);

        if (deletedRecipe.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
        }

        RecipeItemResponseDTO recipeItemResponse = recipeItemService.hydrateRecipeItem(deletedRecipe.get());

        return new ResponseEntity<>(recipeItemResponse, HttpStatus.OK);
    }

    @DeleteMapping(RECIPE_PATH + NAME_PATH + "/{menuItemName}/{condimentName}")
    @Operation(summary = "Deletes a recipe by names", description = "Deletes and returns the recipe item.")
    @ApiResponse(responseCode = "200", description = "Delete recipe item returned successfully")
    public ResponseEntity<RecipeItemResponseDTO> deleteRecipeItemByName(@PathVariable @NotBlank(message = "menuItemName is required") String menuItemName,
                                                                        @PathVariable @NotBlank(message = "condimentName is required") String condimentName) {
        Optional<RecipeItem> deletedRecipe = recipeItemService.deleteRecipeItem(menuItemName, condimentName);

        if (deletedRecipe.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found");
        }

        RecipeItemResponseDTO recipeItemResponse = recipeItemService.hydrateRecipeItem(deletedRecipe.get());

        return new ResponseEntity<>(recipeItemResponse, HttpStatus.OK);
    }
}
