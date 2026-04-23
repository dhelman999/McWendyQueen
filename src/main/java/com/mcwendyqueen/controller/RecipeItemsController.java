package com.mcwendyqueen.controller;

import com.mcwendyqueen.model.ModelMapperUtils;
import com.mcwendyqueen.model.RecipeItem;
import com.mcwendyqueen.model.RecipeItemRequestDTO;
import com.mcwendyqueen.model.RecipeItemResponseDTO;
import com.mcwendyqueen.service.RecipeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.mcwendyqueen.ApiConstants.API_BASE_PATH;
import static com.mcwendyqueen.ApiConstants.RECIPE_PATH;
import static com.mcwendyqueen.ApiConstants.V1_PATH;

@Slf4j
@RestController
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

        List<RecipeItemResponseDTO> response = allRecipeItems.stream()
                .map(ModelMapperUtils::GetRecipeItemResponseDTO)
                .toList();

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(RECIPE_PATH)
    @Operation(summary = "Create a new recipe item", description = "Creates and saves a new recipe item.")
    @ApiResponse(responseCode = "200", description = "Created recipe item returned successfully")
    public ResponseEntity<RecipeItemResponseDTO> createRecipeItem(RecipeItemRequestDTO newRecipeItem) {
        RecipeItem createdRecipeItem = recipeItemService.createRecipeItem(newRecipeItem);
        RecipeItemResponseDTO recipeItemResponse = ModelMapperUtils.GetRecipeItemResponseDTO(createdRecipeItem);

        return new ResponseEntity<>(recipeItemResponse, HttpStatus.OK);
    }

    @DeleteMapping(RECIPE_PATH)
    @Operation(summary = "Deletes a recipe item", description = "Deletes and returns the recipe item.")
    @ApiResponse(responseCode = "200", description = "Delete recipe item returned successfully")
    public ResponseEntity<RecipeItemResponseDTO> deleteRecipeItem(RecipeItemRequestDTO recipeItem) {
        RecipeItem deletedRecipeItem = recipeItemService.deleteRecipeItem(recipeItem);
        RecipeItemResponseDTO recipeItemResponse = ModelMapperUtils.GetRecipeItemResponseDTO(deletedRecipeItem);

        return new ResponseEntity<>(recipeItemResponse, HttpStatus.OK);
    }
}
