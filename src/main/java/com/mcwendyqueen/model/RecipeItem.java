package com.mcwendyqueen.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "recipes", uniqueConstraints = {
@UniqueConstraint(columnNames = {"menuId", "condimentId"})
        })
public class RecipeItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private long menuId;

    private long condimentId;

    public RecipeItem(long menuId, long condimentId) {
        this.menuId = menuId;
        this.condimentId = condimentId;
    }
}
