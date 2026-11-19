package com.example.javabasics.web;

import com.example.javabasics.domain.Customer;
import com.example.javabasics.domain.Sale;

public record SaleUpdateRequest(Long saleId, Long customerId, String note) {

    /** Builds a Sale that was never loaded from the database: it only knows what the request says. */
    public Sale toEntity() {
        return new Sale(saleId, new Customer(customerId, null, null), note);
    }
}
