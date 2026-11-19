package com.example.javabasics.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.Product;
import com.example.javabasics.domain.ProductRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/**
 * Block 10 - Concurrency in JPA. Two "users" are simulated with two nested transactions
 * on two different connections, so the demos are deterministic. Each demo creates its own
 * temporary product and deletes it at the end.
 */
@RestController
@RequestMapping("/demo/locking")
public class LockingDemoController {

    private final EntityManager em;
    private final Tx tx;
    private final ProductRepository repository;

    public LockingDemoController(EntityManager em, Tx tx, ProductRepository repository) {
        this.em = em;
        this.tx = tx;
        this.repository = repository;
    }

    /**
     * Lost update, prevented. B reads the row, A reads it, changes it and commits, then B
     * (which still holds the old version) tries to commit.
     */
    @PostMapping("/optimistic")
    public Map<String, Object> optimistic(@RequestParam(defaultValue = "10") int initialStock) {
        Long id = createTemporaryProduct(initialStock);
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            tx.runVoid(() -> { // transaction B
                Product b = find(id);
                result.put("B_loadedVersion", b.getVersion());

                tx.runNew(() -> { // transaction A starts and commits while B is still open
                    Product a = find(id);
                    result.put("A_loadedVersion", a.getVersion());
                    a.setStock(a.getStock() - 3);
                    return null;
                });
                result.put("versionInDbAfterA", versionInDb(id));

                b.setStock(b.getStock() - 5); // B still works with the old snapshot
            });
            result.put("B_outcome", "committed (without @Version this would be a LOST UPDATE)");
        } catch (RuntimeException e) {
            result.put("B_outcome", e.getClass().getSimpleName());
        }
        result.put("expectedStockIfBothApplied", initialStock - 3 - 5);
        result.put("stockInDbAtTheEnd", tx.run(() -> find(id).getStock()));
        deleteTemporaryProduct(id);
        return result;
    }

    /**
     * Controlled retry: a conflict is injected on the first attempt only. The retry must
     * start a NEW transaction and reload the data, never reuse the failed one.
     */
    @PostMapping("/optimistic-retry")
    public Map<String, Object> optimisticRetry(
            @RequestParam(defaultValue = "5") int increment,
            @RequestParam(defaultValue = "3") int maxAttempts) {
        Long id = createTemporaryProduct(10);
        List<String> log = new ArrayList<>();
        boolean[] conflictInjected = { false };
        boolean success = false;

        for (int attempt = 1; attempt <= maxAttempts && !success; attempt++) {
            try {
                tx.runVoid(() -> {
                    Product product = find(id); // fresh read on every attempt
                    if (!conflictInjected[0]) {
                        conflictInjected[0] = true;
                        tx.runNew(() -> { // someone else updates the same row first
                            Product other = find(id);
                            other.setStock(other.getStock() + 100);
                            return null;
                        });
                    }
                    product.setStock(product.getStock() + increment);
                });
                log.add("attempt " + attempt + ": committed");
                success = true;
            } catch (OptimisticLockingFailureException e) {
                log.add("attempt " + attempt + ": conflict, reloading and trying again");
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("log", log);
        result.put("success", success);
        result.put("finalStock (10 + 100 from the other user + increment)", tx.run(() -> find(id).getStock()));
        deleteTemporaryProduct(id);
        return result;
    }

    /**
     * Pessimistic locking: A locks the row (select ... for update). B asks for the same lock
     * without waiting and fails immediately.
     */
    @PostMapping("/pessimistic")
    public Map<String, Object> pessimistic() {
        Long id = createTemporaryProduct(10);
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            tx.runVoid(() -> { // transaction A
                repository.findForUpdate(id).orElseThrow();
                result.put("A", "holds PESSIMISTIC_WRITE on the row");
                try {
                    tx.runNew(() -> { // transaction B
                        // safety net so a misbehaving driver can never block this demo forever
                        em.createNativeQuery("select set_config('lock_timeout', '2000', true)").getSingleResult();
                        em.find(Product.class, id, LockModeType.PESSIMISTIC_WRITE,
                                Map.of("jakarta.persistence.lock.timeout", 0)); // 0 = do not wait
                        return null;
                    });
                    result.put("B", "got the lock (unexpected)");
                } catch (RuntimeException e) {
                    result.put("B", "could not lock the row: " + e.getClass().getSimpleName());
                }
            });
        } finally {
            deleteTemporaryProduct(id);
        }
        return result;
    }

    private Long createTemporaryProduct(int stock) {
        return tx.run(() -> repository.save(new Product("Locking demo", stock)).getId());
    }

    private void deleteTemporaryProduct(Long id) {
        tx.runVoid(() -> repository.deleteById(id));
    }

    private long versionInDb(Long id) {
        return ((Number) em.createNativeQuery("select version from products where id = :id")
                .setParameter("id", id)
                .getSingleResult()).longValue();
    }

    private Product find(Long id) {
        return em.find(Product.class, id);
    }
}
