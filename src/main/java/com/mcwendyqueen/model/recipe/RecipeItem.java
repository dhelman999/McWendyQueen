package com.mcwendyqueen.model.recipe;

import jakarta.persistence.Column;
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
@UniqueConstraint(columnNames = {"menu_id", "condiment_id"})
        })
public class RecipeItem {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "menu_id")
    private long menuId;

    @Column(name = "condiment_id")
    private long condimentId;

    public RecipeItem(long menuId, long condimentId) {
        this.menuId = menuId;
        this.condimentId = condimentId;
    }
}
