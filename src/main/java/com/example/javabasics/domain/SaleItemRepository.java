package com.example.javabasics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
}
