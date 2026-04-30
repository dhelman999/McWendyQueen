package com.mcwendyqueen.model.condiment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CondimentRepository extends JpaRepository<CondimentItem, Long> {

    Optional<CondimentItem> findByName(String name);

    /* Equivalent SQL:
    SELECT c.*
    FROM condiments c
    LEFT JOIN recipes r ON r.condiment_id = c.id
    WHERE r.menu_id = :menuId
     */
    @Query("""
    select c
    from CondimentItem c
    where c.id in (
        select r.condimentId
        from RecipeItem r
        where r.menuId = :menuId
    )
    """)
    List<CondimentItem> findAllCondimentItemsForMenuItem(@Param("menuId") Long menuId);
}

