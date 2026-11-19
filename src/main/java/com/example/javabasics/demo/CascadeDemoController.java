package com.example.javabasics.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.Customer;
import com.example.javabasics.domain.Product;
import com.example.javabasics.domain.Sale;
import com.example.javabasics.domain.SaleItem;
import com.example.javabasics.web.NotFoundException;

import jakarta.persistence.EntityManager;

/** Block 6 - Cascades and orphanRemoval. Every demo is rolled back. */
@RestController
@RequestMapping("/demo/cascade")
public class CascadeDemoController {

    private final EntityManager em;
    private final Tx tx;
    private final QueryStats stats;

    public CascadeDemoController(EntityManager em, Tx tx, QueryStats stats) {
        this.em = em;
        this.tx = tx;
        this.stats = stats;
    }

    /** Persist ONLY the sale: PERSIST cascades to the two items. */
    @PostMapping("/persist")
    public Map<String, Object> persist(
            @RequestParam(defaultValue = "1") Long customerId,
            @RequestParam(defaultValue = "1") Long productId) {
        QueryStats.Snapshot start = stats.take();
        Map<String, Object> result = tx.runAndRollback(() -> {
            Sale sale = new Sale(find(Customer.class, customerId));
            sale.addItem(new SaleItem(find(Product.class, productId), 1));
            sale.addItem(new SaleItem(find(Product.class, productId), 2));
            em.persist(sale); // only the sale is persisted explicitly
            em.flush();

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("saleId", sale.getId());
            r.put("itemIds", sale.getItems().stream().map(SaleItem::getId).toList());
            return r;
        });
        result.put("inserts", stats.since(start).get("inserts"));
        return result;
    }

    /** Removing an item from the collection deletes the row (orphanRemoval). */
    @PostMapping("/orphan-removal")
    public Map<String, Object> orphanRemoval(@RequestParam(defaultValue = "1") Long saleId) {
        QueryStats.Snapshot start = stats.take();
        Map<String, Object> result = tx.runAndRollback(() -> {
            Sale sale = find(Sale.class, saleId);
            if (sale.getItems().isEmpty()) {
                throw new IllegalArgumentException("Sale " + saleId + " has no items");
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("itemsBefore", sale.getItems().size());
            sale.removeItem(sale.getItems().get(0));
            em.flush();
            r.put("itemsAfter", sale.getItems().size());
            return r;
        });
        result.put("deletes", stats.since(start).get("deletes"));
        return result;
    }

    /** Removing the sale cascades REMOVE to its items: 1 + N deletes. */
    @PostMapping("/remove-sale")
    public Map<String, Object> removeSale(@RequestParam(defaultValue = "1") Long saleId) {
        QueryStats.Snapshot start = stats.take();
        Map<String, Object> result = tx.runAndRollback(() -> {
            Sale sale = find(Sale.class, saleId);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("items", sale.getItems().size());
            em.remove(sale);
            em.flush();
            return r;
        });
        result.put("deletes", stats.since(start).get("deletes"));
        return result;
    }

    /**
     * The trap: TrapLine -> Product has CascadeType.REMOVE, but a Product is shared.
     * shared=false: deleting one line silently deletes the product too. shared=true: the
     * flush fails because the second line now points to a product that was just removed.
     */
    @PostMapping("/trap")
    public Map<String, Object> trap(@RequestParam(defaultValue = "false") boolean shared) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            tx.runAndRollback(() -> {
                Product product = new Product("Shared product", 5);
                em.persist(product);
                TrapLine line = new TrapLine(product);
                em.persist(line);
                if (shared) {
                    em.persist(new TrapLine(product));
                }
                em.remove(line);
                em.flush();
                Number rows = (Number) em.createNativeQuery("select count(*) from products where id = :id")
                        .setParameter("id", product.getId())
                        .getSingleResult();
                result.put("productStillExists", rows.longValue() == 1);
                return null;
            });
        } catch (RuntimeException e) {
            result.put("error", e.getClass().getSimpleName());
            result.put("detail", List.of(String.valueOf(e.getMessage()).split("\n")).get(0));
        }
        return result;
    }

    private <T> T find(Class<T> type, Long id) {
        T entity = em.find(type, id);
        if (entity == null) {
            throw new NotFoundException(type.getSimpleName() + " " + id);
        }
        return entity;
    }
}
