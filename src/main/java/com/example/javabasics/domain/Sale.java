package com.example.javabasics.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // OWNING side: this entity holds the foreign key column (sales.customer_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(length = 255)
    private String note;

    // INVERSE side: mappedBy says "the foreign key lives in SaleItem.sale"
    @OneToMany(mappedBy = "sale")
    private List<SaleItem> items = new ArrayList<>();

    protected Sale() {
    }

    public Sale(Customer customer) {
        this.customer = customer;
    }

    /** Keeps both sides of the bidirectional relation in sync. */
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<SaleItem> getItems() {
        return items;
    }
}
