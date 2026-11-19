package com.example.javabasics.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.SaleService;
import com.example.javabasics.web.SaleResponse;

/** Block 7 - The same listing with three different fetch plans. Compare "sqlStatements". */
@RestController
@RequestMapping("/demo/fetch")
public class FetchDemoController {

    private final SaleService service;
    private final QueryStats stats;

    public FetchDemoController(SaleService service, QueryStats stats) {
        this.service = service;
        this.stats = stats;
    }

    @GetMapping("/n-plus-one")
    public Map<String, Object> nPlusOne() {
        return measure("findAll() and then LAZY access to customer, items and products", service::findAll);
    }

    @GetMapping("/entity-graph")
    public Map<String, Object> entityGraph() {
        return measure("@EntityGraph(customer, items, items.product)", service::findAllWithEntityGraph);
    }

    @GetMapping("/join-fetch")
    public Map<String, Object> joinFetch() {
        return measure("JPQL with join fetch", service::findAllWithJoinFetch);
    }

    private Map<String, Object> measure(String strategy, Supplier<List<SaleResponse>> loader) {
        QueryStats.Snapshot start = stats.take();
        long begin = System.nanoTime();
        List<SaleResponse> sales = loader.get();
        long elapsedMs = (System.nanoTime() - begin) / 1_000_000;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("strategy", strategy);
        result.put("sales", sales.size());
        result.putAll(stats.since(start));
        result.put("elapsedMs", elapsedMs);
        return result;
    }
}
