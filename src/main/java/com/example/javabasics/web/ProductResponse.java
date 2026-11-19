package com.example.javabasics.web;

import com.example.javabasics.domain.Product;

public record ProductResponse(Long id, String name, int stock, boolean active, long version) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getStock(),
                product.isActive(), product.getVersion());
    }
}
