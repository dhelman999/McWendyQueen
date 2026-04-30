package com.mcwendyqueen.model.condiment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CondimentItemResponseDTO {
    private long id;

    private String name;
}
