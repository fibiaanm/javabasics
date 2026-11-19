package com.example.javabasics.web;

import java.util.List;

public record CreateSaleRequest(Long customerId, List<Line> items) {

    public record Line(Long productId, int quantity) {
    }
}
