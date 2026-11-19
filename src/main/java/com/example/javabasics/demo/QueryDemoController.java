package com.example.javabasics.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.Sale;
import com.example.javabasics.domain.SaleRepository;
import com.example.javabasics.domain.SaleSummary;

/** Block 8 - JPQL, DTO projections and pagination. Compare "sqlStatements" and "entitiesLoaded". */
@RestController
@RequestMapping("/demo/queries")
public class QueryDemoController {

    private final SaleRepository repository;
    private final Tx tx;
    private final QueryStats stats;

    public QueryDemoController(SaleRepository repository, Tx tx, QueryStats stats) {
        this.repository = repository;
        this.tx = tx;
        this.stats = stats;
    }

    @GetMapping("/by-customer")
    public Map<String, Object> byCustomer(@RequestParam(defaultValue = "1") Long customerId) {
        return measure("JPQL: where s.customer.id = :customerId", () -> {
            List<Sale> sales = repository.findByCustomer(customerId);
            return Map.of("saleIds", sales.stream().map(Sale::getId).toList());
        });
    }

    @GetMapping("/summaries")
    public Map<String, Object> summaries() {
        return measure("DTO projection (new SaleSummary(...))", () -> {
            List<SaleSummary> rows = repository.findSummaries();
            return Map.of("rows", rows.size(), "firstRow", rows.isEmpty() ? "none" : rows.get(0));
        });
    }

    @GetMapping("/page")
    public Map<String, Object> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return measure("Page<SaleSummary>: data query + COUNT query", () -> {
            Page<SaleSummary> result = repository.findSummaryPage(PageRequest.of(page, size));
            return Map.of(
                    "rows", result.getNumberOfElements(),
                    "totalElements", result.getTotalElements(),
                    "totalPages", result.getTotalPages(),
                    "hasNext", result.hasNext());
        });
    }

    @GetMapping("/slice")
    public Map<String, Object> slice(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return measure("Slice<SaleSummary>: no COUNT query", () -> {
            Slice<SaleSummary> result = repository.findSummarySlice(PageRequest.of(page, size));
            return Map.of("rows", result.getNumberOfElements(), "hasNext", result.hasNext());
        });
    }

    @GetMapping("/page-fetch-collection")
    public Map<String, Object> pageFetchCollection(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return measure("Page<Sale> + join fetch of a collection (entities are the whole Sale graph)", () -> {
            Page<Sale> result = repository.findPageFetchingItems(PageRequest.of(page, size));
            return Map.of(
                    "rowsReturned", result.getNumberOfElements(),
                    "totalElements", result.getTotalElements());
        });
    }

    private Map<String, Object> measure(String strategy, Supplier<Map<String, Object>> query) {
        QueryStats.Snapshot start = stats.take();
        Map<String, Object> outcome = tx.run(query);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("strategy", strategy);
        result.putAll(outcome);
        result.putAll(stats.since(start));
        return result;
    }
}
