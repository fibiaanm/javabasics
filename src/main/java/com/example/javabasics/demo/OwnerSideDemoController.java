package com.example.javabasics.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.Customer;
import com.example.javabasics.domain.Sale;
import com.example.javabasics.web.NotFoundException;

import jakarta.persistence.EntityManager;

/** Block 5 - Which side of a bidirectional relation owns the foreign key? */
@RestController
@RequestMapping("/demo")
public class OwnerSideDemoController {

    private final EntityManager em;
    private final Tx tx;

    public OwnerSideDemoController(EntityManager em, Tx tx) {
        this.em = em;
        this.tx = tx;
    }

    /**
     * Moves a sale to another customer in two different ways and reads the real
     * foreign key from the database (native SQL) after flushing. Both are rolled back.
     */
    @GetMapping("/owner-side")
    public Map<String, Object> ownerSide(
            @RequestParam(defaultValue = "1") Long saleId,
            @RequestParam(defaultValue = "2") Long newCustomerId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("customerIdBefore", tx.run(() -> foreignKey(saleId)));
        result.put("newCustomerId", newCustomerId);

        result.put("onlyCustomer.getSales().add(sale) -> customerIdInDb", tx.runAndRollback(() -> {
            Sale sale = find(Sale.class, saleId);
            Customer other = find(Customer.class, newCustomerId);
            other.getSales().add(sale); // inverse side only
            em.flush();
            return foreignKey(saleId);
        }));

        result.put("sale.setCustomer(customer) -> customerIdInDb", tx.runAndRollback(() -> {
            Sale sale = find(Sale.class, saleId);
            Customer other = find(Customer.class, newCustomerId);
            sale.setCustomer(other); // owning side
            em.flush();
            return foreignKey(saleId);
        }));
        return result;
    }

    private Long foreignKey(Long saleId) {
        Number fk = (Number) em.createNativeQuery("select customer_id from sales where id = :id")
                .setParameter("id", saleId)
                .getSingleResult();
        return fk.longValue();
    }

    private <T> T find(Class<T> type, Long id) {
        T entity = em.find(type, id);
        if (entity == null) {
            throw new NotFoundException(type.getSimpleName() + " " + id);
        }
        return entity;
    }
}
