package com.example.javabasics.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Pessimistic locking: "select ... for update". Other writers wait (or fail) until we commit. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdate(@Param("id") Long id);

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
