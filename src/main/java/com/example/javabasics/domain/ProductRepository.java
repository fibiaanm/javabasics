package com.example.javabasics.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Bulk update: ONE statement runs directly in the database and bypasses the persistence
     * context. Entities already loaded keep their old state (they are stale).
     */
    @Transactional
    @Modifying
    @Query("update Product p set p.active = false where p.stock = 0")
    int deactivateWithoutStock();

    /** Same statement, but the persistence context is flushed before and cleared after. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.active = false where p.stock = 0")
    int deactivateWithoutStockAndClear();
}
