package com.example.javabasics.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.Product;
import com.example.javabasics.web.NotFoundException;

import jakarta.persistence.EntityManager;

/** Block 3 - Dirty checking, flush and transactions. */
@RestController
@RequestMapping("/demo")
public class FlushDemoController {

    private final EntityManager em;
    private final Tx tx;
    private final QueryStats stats;

    public FlushDemoController(EntityManager em, Tx tx, QueryStats stats) {
        this.em = em;
        this.tx = tx;
        this.stats = stats;
    }

    /**
     * Dirty checking: change a managed entity, never call save() -> UPDATE at commit.
     * Setting the same value again produces no UPDATE (snapshot comparison).
     */
    @PostMapping("/dirty-checking")
    public Map<String, Object> dirtyChecking(
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "50") int stock) {
        Map<String, Object> result = new LinkedHashMap<>();

        QueryStats.Snapshot start = stats.take();
        tx.runVoid(() -> find(productId).setStock(stock));
        result.put("changeValue_updates", stats.since(start).get("updates"));

        QueryStats.Snapshot start2 = stats.take();
        tx.runVoid(() -> find(productId).setStock(stock)); // same value as already stored
        result.put("sameValueAgain_updates", stats.since(start2).get("updates"));
        return result;
    }

    /**
     * Follows the UPDATE counter step by step: when exactly does Hibernate flush?
     * With rollback=true the changes are executed but never committed.
     */
    @PostMapping("/flush")
    public Map<String, Object> flush(
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "true") boolean rollback) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> steps = new ArrayList<>();
        result.put("stockInDbBefore", tx.run(() -> find(productId).getStock()));

        QueryStats.Snapshot start = stats.take();
        tx.runWithStatus(status -> {
            Product product = find(productId);

            product.setStock(10);
            product.setStock(20);
            product.setStock(30);
            steps.add(step("setStock(10), setStock(20), setStock(30)", start));

            em.createQuery("select count(p) from Product p", Long.class).getSingleResult();
            steps.add(step("JPQL query on Product (AUTO flush happens BEFORE the query)", start));

            product.setStock(40);
            em.flush();
            steps.add(step("setStock(40) + em.flush()", start));

            if (rollback) {
                status.setRollbackOnly();
            }
            return null;
        });
        result.put("steps", steps);
        result.put("rolledBack", rollback);
        result.put("stockInDbAfter", tx.run(() -> find(productId).getStock()));
        return result;
    }

    private Map<String, Object> step(String operation, QueryStats.Snapshot start) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("after", operation);
        step.put("updatesExecutedSoFar", stats.since(start).get("updates"));
        return step;
    }

    private Product find(Long id) {
        Product product = em.find(Product.class, id);
        if (product == null) {
            throw new NotFoundException("Product " + id);
        }
        return product;
    }
}
