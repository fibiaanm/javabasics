package com.example.javabasics.demo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.web.SaleUpdateRequest;

import jakarta.persistence.EntityManager;

/**
 * Block 11 - Runs the broken and the fixed update and reports the damage.
 * By default everything is rolled back (rollback=true), so the demo can be repeated.
 */
@RestController
@RequestMapping("/challenge")
public class ChallengeController {

    private final ChallengeService service;
    private final EntityManager em;
    private final Tx tx;
    private final QueryStats stats;

    public ChallengeController(ChallengeService service, EntityManager em, Tx tx, QueryStats stats) {
        this.service = service;
        this.em = em;
        this.tx = tx;
        this.stats = stats;
    }

    @PutMapping("/broken")
    public Map<String, Object> broken(@RequestBody SaleUpdateRequest request,
            @RequestParam(defaultValue = "true") boolean rollback) {
        return run("updateSaleBroken", request, rollback, service::updateSaleBroken);
    }

    @PutMapping("/fixed")
    public Map<String, Object> fixed(@RequestBody SaleUpdateRequest request,
            @RequestParam(defaultValue = "true") boolean rollback) {
        return run("updateSaleFixed", request, rollback, service::updateSaleFixed);
    }

    private Map<String, Object> run(String method, SaleUpdateRequest request, boolean rollback,
            Consumer<SaleUpdateRequest> action) {
        QueryStats.Snapshot start = stats.take();
        java.util.function.Supplier<Map<String, Object>> body = () -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("method", method);
            result.put("itemsBefore", itemCount(request.saleId()));
            result.put("noteBefore", note(request.saleId()));
            action.accept(request);
            result.put("itemsAfter", itemCount(request.saleId()));
            result.put("noteAfter", note(request.saleId()));
            return result;
        };
        Map<String, Object> result = rollback ? tx.runAndRollback(body) : tx.run(body);
        result.put("rolledBack", rollback);
        result.putAll(stats.since(start));
        return result;
    }

    private long itemCount(Long saleId) {
        return em.createQuery("select count(i) from SaleItem i where i.sale.id = :id", Long.class)
                .setParameter("id", saleId)
                .getSingleResult();
    }

    private String note(Long saleId) {
        java.util.List<String> rows = em.createQuery("select s.note from Sale s where s.id = :id", String.class)
                .setParameter("id", saleId)
                .getResultList(); // a stream's findFirst() would throw on a null note
        return rows.isEmpty() ? null : rows.get(0);
    }
}
