package com.mcwendyqueen.model.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeItemRequestDTO {
    @NotBlank(message = "menu item name is required")
    @Size(max = 25, message = "menu item name must be at most 25 characters")
    private String menuItemName;

    @NotBlank(message = "condiment name is required")
    @Size(max = 25, message = "condiment name must be at most 25 characters")
    private String condimentName;
}
