package com.example.javabasics.demo;

import com.example.javabasics.domain.Product;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A DELIBERATELY WRONG mapping used only by the cascade demo:
 * REMOVE cascades from a line to the Product it points to, and a Product is a SHARED
 * entity (many lines can reference it). Never copy this.
 */
@Entity
@Table(name = "trap_lines")
public class TrapLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    protected TrapLine() {
    }

    public TrapLine(Product product) {
        this.product = product;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }
}
