package com.example.javabasics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    // JPA requires a no-arg constructor (it can be protected)
    protected Product() {
    }

    public Product(String name, int stock) {
        this.name = name;
        this.stock = stock;
    }

    /**
     * Builds an object that carries an id but is NOT loaded from the database.
     * This is what "request.toEntity()" looks like: handing it to merge()/save()
     * overwrites every column with whatever this object contains.
     */
    public Product(Long id, String name, int stock) {
        this.id = id;
        this.name = name;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
