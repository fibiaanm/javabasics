package com.example.javabasics.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.javabasics.domain.Product;

import jakarta.persistence.EntityManager;

/**
 * Block 2 - Persistence context and entity states.
 * Every endpoint runs its transactions explicitly (see Tx) and reports what happened.
 */
@RestController
@RequestMapping("/demo")
public class PersistenceContextDemoController {

    private final EntityManager em;
    private final Tx tx;
    private final QueryStats stats;

    public PersistenceContextDemoController(EntityManager em, Tx tx, QueryStats stats) {
        this.em = em;
        this.tx = tx;
        this.stats = stats;
    }

    /**
     * Walks one object through TRANSIENT -> MANAGED -> DETACHED -> MANAGED -> REMOVED.
     * Everything is rolled back, no data is left behind.
     */
    @PostMapping("/states")
    public Map<String, Object> states() {
        List<Map<String, Object>> steps = new ArrayList<>();
        tx.runAndRollback(() -> {
            Product product = new Product("Demo product", 1);
            steps.add(step("new Product(...)", "TRANSIENT", product));

            em.persist(product);
            steps.add(step("em.persist(product)", "MANAGED", product));

            em.detach(product);
            steps.add(step("em.detach(product)", "DETACHED", product));

            Product reloaded = em.find(Product.class, product.getId());
            steps.add(step("em.find(...) -> a NEW instance", "MANAGED", reloaded));
            steps.get(steps.size() - 1).put("sameObjectAsDetachedOne", reloaded == product);

            em.remove(reloaded);
            steps.add(step("em.remove(reloaded)", "REMOVED (deleted at flush/commit)", reloaded));
            return null;
        });
        return Map.of("steps", steps);
    }

    /** First-level cache: same id + same persistence context = same Java instance. */
    @GetMapping("/identity")
    public Map<String, Object> identity(@RequestParam(defaultValue = "1") Long productId) {
        Map<String, Object> result = new LinkedHashMap<>();
        QueryStats.Snapshot start = stats.take();

        tx.runVoid(() -> {
            Product first = find(productId);
            Product second = find(productId);
            result.put("find_then_find_sameInstance", first == second);
            result.put("sqlAfterTwoFinds", stats.since(start).get("sqlStatements"));

            Product viaJpql = em.createQuery("select p from Product p where p.id = :id", Product.class)
                    .setParameter("id", productId)
                    .getSingleResult();
            result.put("jpql_sameInstance", viaJpql == first);
            result.put("sqlAfterJpql", stats.since(start).get("sqlStatements"));
        });

        Product a = tx.run(() -> find(productId));
        Product b = tx.run(() -> find(productId));
        result.put("twoTransactions_sameInstance", a == b);
        return result;
    }

    /** Changing an object that is no longer managed does NOT produce an UPDATE. */
    @GetMapping("/detached-change")
    public Map<String, Object> detachedChange(
            @RequestParam(defaultValue = "1") Long productId,
            @RequestParam(defaultValue = "999") int newStock) {
        Map<String, Object> result = new LinkedHashMap<>();
        QueryStats.Snapshot start = stats.take();

        Product detached = tx.run(() -> find(productId)); // transaction closed => DETACHED
        result.put("stockInDbBefore", detached.getStock());

        detached.setStock(newStock); // outside any transaction

        result.put("stockInMemory", detached.getStock());
        result.put("stockInDbAfter", tx.run(() -> find(productId).getStock()));
        result.put("updatesExecuted", stats.since(start).get("updates"));
        return result;
    }

    private Product find(Long id) {
        Product product = em.find(Product.class, id);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + id + " not found");
        }
        return product;
    }

    private Map<String, Object> step(String operation, String state, Product product) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("operation", operation);
        step.put("state", state);
        step.put("id", product.getId());
        step.put("em.contains(product)", em.contains(product));
        return step;
    }
}
