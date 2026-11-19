package com.example.javabasics.web;

/** Partial update: a null field means "leave it as it is". */
public record ProductPatchRequest(String name, Integer stock) {
}
