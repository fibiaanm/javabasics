package com.example.javabasics.domain;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    /** Fetch plan declared as an EntityGraph: the query stays "select s from Sale s". */
    @EntityGraph(attributePaths = { "customer", "items", "items.product" })
    @Query("select s from Sale s")
    List<Sale> findAllWithEntityGraph();

    /**
     * Fetch plan written in the query itself. "left join fetch" keeps sales without items.
     * distinct removes the duplicated Sale rows that the join over items produces.
     */
    @Query("""
            select distinct s
              from Sale s
              join fetch s.customer
              left join fetch s.items i
              left join fetch i.product
            """)
    List<Sale> findAllJoinFetch();
}
