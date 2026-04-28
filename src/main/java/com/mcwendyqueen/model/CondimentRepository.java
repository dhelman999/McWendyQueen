package com.mcwendyqueen.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CondimentRepository extends JpaRepository<CondimentItem, Long> {

    Optional<CondimentItem> findByName(String name);
}

