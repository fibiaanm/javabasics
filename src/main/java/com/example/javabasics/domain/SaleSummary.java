package com.example.javabasics.domain;

/** A projection: only the columns a listing needs, no entities, no persistence context. */
public record SaleSummary(Long id, String customer, Long itemCount, Long totalUnits) {
}
