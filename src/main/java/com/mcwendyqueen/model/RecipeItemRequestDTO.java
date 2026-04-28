package com.mcwendyqueen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeItemRequestDTO {
    private String menuItemName;

    private String condimentName;
}
