package com.mcwendyqueen.model.condiment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CondimentItemRequestDTO {
    @NotBlank(message = "name is required")
    @Size(max = 25, message = "name must be at most 25 characters")
    private String name;
}
