package com.mcwendyqueen.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipeRepository extends JpaRepository<RecipeItem, Long> {

    Optional<RecipeItem> findByMenuIdAndCondimentId (long menuId, long condimentId);
}

