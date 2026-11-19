package com.example.javabasics.demo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.javabasics.domain.CustomerRepository;
import com.example.javabasics.domain.Sale;
import com.example.javabasics.domain.SaleRepository;
import com.example.javabasics.web.NotFoundException;
import com.example.javabasics.web.SaleUpdateRequest;

import jakarta.persistence.EntityManager;

/** Block 11 - Final challenge: find what is wrong with updateSaleBroken, then compare with the fix. */
@Service
public class ChallengeService {

    private final EntityManager entityManager;
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;

    public ChallengeService(EntityManager entityManager, SaleRepository saleRepository,
            CustomerRepository customerRepository) {
        this.entityManager = entityManager;
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
    }

    // ------------------------------------------------------------------------------------
    // THE CHALLENGE: what is wrong with this method?
    // ------------------------------------------------------------------------------------
    @Transactional
    public void updateSaleBroken(SaleUpdateRequest request) {
        Sale sale = request.toEntity();

        entityManager.merge(sale);

        sale.getCustomer().getSales().size();

        entityManager.flush();

        sale.setNote("final note");

        entityManager.flush();
    }

    // ------------------------------------------------------------------------------------
    // THE FIX
    // ------------------------------------------------------------------------------------
    @Transactional
    public void updateSaleFixed(SaleUpdateRequest request) {
        Sale sale = saleRepository.findById(request.saleId())
                .orElseThrow(() -> new NotFoundException("Sale " + request.saleId()));

        if (request.customerId() != null) {
            sale.setCustomer(customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Customer " + request.customerId())));
        }
        sale.setNote(request.note());
        // Managed entity + dirty checking: one UPDATE, only the columns that changed, no save().
        // Next step for a real system: @Version on Sale to reject stale updates (see block 10).
    }
}
