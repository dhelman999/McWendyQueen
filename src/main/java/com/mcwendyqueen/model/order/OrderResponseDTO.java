package com.mcwendyqueen.model.order;

import com.mcwendyqueen.model.condiment.CondimentItemResponseDTO;
import com.mcwendyqueen.model.menuitem.MenuItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private long id;

    private String name;

    private MenuItem baseMenuItem;

    private Set<CondimentItemResponseDTO> condiments;
}
