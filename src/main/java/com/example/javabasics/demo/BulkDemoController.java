package com.example.javabasics.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.Product;
import com.example.javabasics.domain.ProductRepository;

import jakarta.persistence.EntityManager;

/** Block 9 - Bulk updates versus the persistence context. Rolled back, no data left behind. */
@RestController
@RequestMapping("/demo")
public class BulkDemoController {

    private final EntityManager em;
    private final Tx tx;
    private final ProductRepository repository;

    public BulkDemoController(EntityManager em, Tx tx, ProductRepository repository) {
        this.em = em;
        this.tx = tx;
        this.repository = repository;
    }

    /**
     * Loads a product with stock 0 into the persistence context, then deactivates every
     * product without stock with a bulk UPDATE. What does the Java object say afterwards?
     */
    @PostMapping("/bulk")
    public Map<String, Object> bulk(@RequestParam(defaultValue = "false") boolean clear) {
        return tx.runAndRollback(() -> {
            Product product = repository.save(new Product("Bulk demo", 0)); // managed, active = true

            int rows = clear ? repository.deactivateWithoutStockAndClear() : repository.deactivateWithoutStock();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("clearAutomatically", clear);
            result.put("rowsUpdatedByTheBulkUpdate", rows);
            result.put("productIsStillManaged", em.contains(product));
            result.put("activeInMemory(theObjectWeHold)", product.isActive());
            result.put("activeInDatabase", (Boolean) em.createNativeQuery(
                    "select active from products where id = :id")
                    .setParameter("id", product.getId())
                    .getSingleResult());
            result.put("activeAfter_em.find", em.find(Product.class, product.getId()).isActive());
            return result;
        });
    }
}
