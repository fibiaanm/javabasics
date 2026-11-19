package com.example.javabasics.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.Product;
import com.example.javabasics.web.NotFoundException;

import jakarta.persistence.EntityManager;

/** Block 4 - persist() versus merge(). */
@RestController
@RequestMapping("/demo")
public class MergeDemoController {

    private final EntityManager em;
    private final Tx tx;

    public MergeDemoController(EntityManager em, Tx tx) {
        this.em = em;
        this.tx = tx;
    }

    /**
     * merge() does not make the object you pass in managed: it copies its state onto
     * another (managed) instance and returns that one.
     */
    @GetMapping("/merge")
    public Map<String, Object> merge(@RequestParam(defaultValue = "1") Long productId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Product detached = tx.run(() -> find(productId)); // transaction closed => DETACHED

        tx.runAndRollback(() -> {
            Product managedCopy = em.merge(detached);
            result.put("merge_returnsTheSameInstance", managedCopy == detached);
            result.put("em.contains(theObjectWePassed)", em.contains(detached));
            result.put("em.contains(theObjectMergeReturned)", em.contains(managedCopy));
            return null;
        });

        try {
            tx.runAndRollback(() -> {
                em.persist(detached);
                return null;
            });
            result.put("persist_onDetached", "no error");
        } catch (RuntimeException e) {
            result.put("persist_onDetached", e.getClass().getSimpleName() + ": " + shorten(e.getMessage()));
        }
        return result;
    }

    private Product find(Long id) {
        Product product = em.find(Product.class, id);
        if (product == null) {
            throw new NotFoundException("Product " + id);
        }
        return product;
    }

    private String shorten(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 160 ? message.substring(0, 160) + "..." : message;
    }
}
