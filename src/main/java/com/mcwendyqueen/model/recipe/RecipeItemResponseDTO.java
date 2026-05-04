package com.mcwendyqueen.model.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeItemResponseDTO {
    private long id;

    private long menuId;

    @NonNull
    private String menuItemName;

    private long condimentId;

    @NonNull
    private String condimentName;
}
