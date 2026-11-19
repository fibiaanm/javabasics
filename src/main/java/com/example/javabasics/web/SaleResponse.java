package com.example.javabasics.web;

import java.time.Instant;
import java.util.List;

import com.example.javabasics.domain.Sale;

public record SaleResponse(Long id, Long customerId, String customerName, Instant createdAt,
        String note, List<ItemResponse> items) {

    public record ItemResponse(Long productId, String productName, int quantity) {
    }

    /** Touches customer, items and every product: with LAZY relations each access may hit the DB. */
    public static SaleResponse from(Sale sale) {
        List<ItemResponse> items = sale.getItems().stream()
                .map(item -> new ItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity()))
                .toList();
        return new SaleResponse(
                sale.getId(),
                sale.getCustomer().getId(),
                sale.getCustomer().getName(),
                sale.getCreatedAt(),
                sale.getNote(),
                items);
    }
}
