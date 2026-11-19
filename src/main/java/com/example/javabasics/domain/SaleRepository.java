package com.example.javabasics.domain;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    String SUMMARY_QUERY = """
            select new com.example.javabasics.domain.SaleSummary(
                       s.id, c.name, count(i), coalesce(sum(i.quantity), 0L))
              from Sale s
              join s.customer c
              left join s.items i
             group by s.id, c.name
             order by s.id
            """;

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

    // ---- Block 8: JPQL queries -------------------------------------------------------

    /** JPQL talks about entities and properties, not tables and columns. */
    @Query("select s from Sale s where s.customer.id = :customerId order by s.id")
    List<Sale> findByCustomer(@Param("customerId") Long customerId);

    /** DTO projection: one statement, nothing is loaded into the persistence context. */
    @Query(SUMMARY_QUERY)
    List<SaleSummary> findSummaries();

    /** Page: the data query plus an automatic (or explicit) COUNT query. */
    @Query(value = SUMMARY_QUERY, countQuery = "select count(s) from Sale s")
    Page<SaleSummary> findSummaryPage(Pageable pageable);

    /** Slice: no COUNT, it only asks for one extra row to know if there is a next page. */
    @Query(SUMMARY_QUERY)
    Slice<SaleSummary> findSummarySlice(Pageable pageable);

    /**
     * Pagination combined with a collection fetch join. Hibernate 7 applies the limit to the
     * root entities in a subquery and joins the items afterwards. Older versions (and some
     * query shapes, e.g. fetching two collections) paginate IN MEMORY: they load every row
     * and cut the page in Java, logging HHH000104.
     */
    @Query(value = "select s from Sale s join fetch s.items", countQuery = "select count(s) from Sale s")
    Page<Sale> findPageFetchingItems(Pageable pageable);
}
