package com.mcwendyqueen.model.menuitem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemRequestDTO {
    private long id;

    private String name;
}
