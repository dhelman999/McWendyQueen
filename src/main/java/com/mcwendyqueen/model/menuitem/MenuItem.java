package com.mcwendyqueen.model.menuitem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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
@Table(name = "menuitems", uniqueConstraints = {
                @UniqueConstraint(columnNames = {"name"})
        })
public class MenuItem {
    public MenuItem(long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false, length = 25)
    @NotBlank(message = "name is required")
    @Size(max = 25, message = "name must be at most 25 characters")
    private String name;

    @OneToOne(mappedBy = "menuItem", cascade = CascadeType.ALL)
    private MenuItemDuration miDuration;

    public MenuItem(String name) {
        this.name = name;
    }
}
