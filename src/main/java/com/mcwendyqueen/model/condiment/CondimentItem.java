package com.mcwendyqueen.model.condiment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "condiments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"})
})
public class CondimentItem {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false, length = 25)
    @NotBlank(message = "name is required")
    @Size(max = 25, message = "name must be at most 25 characters")
    private String name;

    public CondimentItem(String name) {
        this.name = name;
    }
}
