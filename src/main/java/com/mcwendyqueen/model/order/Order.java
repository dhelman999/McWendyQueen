package com.mcwendyqueen.model.order;

import com.mcwendyqueen.model.condiment.CondimentItem;
import com.mcwendyqueen.model.menuitem.MenuItem;
import com.mcwendyqueen.model.order.OrderStatus.OrderStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    @JoinColumn(name = "base_menu_item")
    private MenuItem baseMenuItem;

    @ManyToMany
    @JoinTable(
            name = "order_condiments",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "condiment_id")
    )
    private Set<CondimentItem> condiments;

    @Column(name = "created_time")
    private long createdTime;

    @Column(name = "completed_time")
    private long completedTime;

    @Column(name = "order_status")
    private OrderStatusEnum orderStatus;

    public Order() {
        createdTime =  System.currentTimeMillis();
        condiments = new HashSet<>();
    }

    public Order(String name) {
        this.name = name;
        createdTime =  System.currentTimeMillis();
        condiments = new HashSet<>();
    }

    public CondimentItem removeCondiment(CondimentItem condimentToRemove) {
        boolean removed = condiments.remove(condimentToRemove);

        return removed ? condimentToRemove : null;
    }
}
