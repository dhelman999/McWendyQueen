package com.mcwendyqueen.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeItemRequestDTO {
    private long id;

    private long menuId;

    private long condimentId;
}
