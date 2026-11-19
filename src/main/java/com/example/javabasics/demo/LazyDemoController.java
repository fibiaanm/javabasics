package com.example.javabasics.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Hibernate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.Product;
import com.example.javabasics.domain.Sale;
import com.example.javabasics.web.NotFoundException;

import jakarta.persistence.EntityManager;

/** Block 7 - What LAZY really is: proxies and the LazyInitializationException. */
@RestController
@RequestMapping("/demo/lazy")
public class LazyDemoController {

    private final EntityManager em;
    private final Tx tx;
    private final QueryStats stats;

    public LazyDemoController(EntityManager em, Tx tx, QueryStats stats) {
        this.em = em;
        this.tx = tx;
        this.stats = stats;
    }

    /** getReference() returns a proxy: no SQL until a property other than the id is read. */
    @GetMapping("/proxy")
    public Map<String, Object> proxy(@RequestParam(defaultValue = "1") Long productId) {
        Map<String, Object> result = new LinkedHashMap<>();
        QueryStats.Snapshot start = stats.take();
        tx.runVoid(() -> {
            Product reference = em.getReference(Product.class, productId);
            result.put("afterGetReference_sql", stats.since(start).get("sqlStatements"));
            result.put("afterGetReference_initialized", Hibernate.isInitialized(reference));

            reference.getId();
            result.put("afterGetId_sql", stats.since(start).get("sqlStatements"));

            reference.getName();
            result.put("afterGetName_sql", stats.since(start).get("sqlStatements"));
            result.put("afterGetName_initialized", Hibernate.isInitialized(reference));
        });
        return result;
    }

    /** Touching a lazy association after the transaction (and session) is closed. */
    @GetMapping("/outside-tx")
    public Map<String, Object> outsideTransaction(@RequestParam(defaultValue = "1") Long saleId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Sale sale = tx.run(() -> find(saleId)); // the session is closed when this returns

        result.put("items.size()", attempt(() -> sale.getItems().size()));
        result.put("customer.getName()", attempt(() -> sale.getCustomer().getName()));

        Sale initialized = tx.run(() -> {
            Sale loaded = find(saleId);
            Hibernate.initialize(loaded.getItems()); // load the collection while the session is open
            return loaded;
        });
        result.put("items.size() after Hibernate.initialize inside the tx", initialized.getItems().size());
        return result;
    }

    private Sale find(Long id) {
        Sale sale = em.find(Sale.class, id);
        if (sale == null) {
            throw new NotFoundException("Sale " + id);
        }
        return sale;
    }

    private Object attempt(java.util.function.Supplier<Object> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }
}
