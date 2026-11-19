package com.example.javabasics.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    // INVERSE side of Sale.customer: changing this list alone does NOT change the foreign key
    @OneToMany(mappedBy = "customer")
    private List<Sale> sales = new ArrayList<>();

    protected Customer() {
    }

    /** Keeps both sides of the bidirectional relation in sync. */
    public void addSale(Sale sale) {
        sales.add(sale);
        sale.setCustomer(this);
    }

    public List<Sale> getSales() {
        return sales;
    }

    public Customer(String name, String email) {
        this.name = name;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
