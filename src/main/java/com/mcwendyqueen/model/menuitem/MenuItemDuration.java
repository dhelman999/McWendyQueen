package com.mcwendyqueen.model.menuitem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "menuItemDuration", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"menu_id"})
})
public class MenuItemDuration {
    @Id
    private long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "menu_id")
    private MenuItem menuItem;

    @Column(name = "processingTime")
    private long processingTime;
}
