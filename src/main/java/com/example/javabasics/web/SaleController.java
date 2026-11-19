package com.example.javabasics.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.domain.SaleService;

@RestController
@RequestMapping("/sales")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse create(@RequestBody CreateSaleRequest request) {
        return service.create(request);
    }

    /** NAIVE listing: look at the console, every sale triggers more queries. */
    @GetMapping
    public List<SaleResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SaleResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
