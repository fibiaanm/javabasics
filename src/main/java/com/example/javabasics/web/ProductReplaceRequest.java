package com.example.javabasics.web;

import com.example.javabasics.domain.Product;

/**
 * Body of the naive PUT. "stock" is nullable so a missing field reaches toEntity(),
 * where the classic shortcut "null means 0" silently turns it into data loss.
 */
public record ProductReplaceRequest(String name, Integer stock) {

    public Product toEntity(Long id) {
        return new Product(id, name, stock == null ? 0 : stock);
    }
}
