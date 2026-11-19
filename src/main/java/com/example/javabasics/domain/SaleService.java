package com.example.javabasics.domain;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.javabasics.web.CreateSaleRequest;
import com.example.javabasics.web.InsufficientStockException;
import com.example.javabasics.web.NotFoundException;
import com.example.javabasics.web.SaleResponse;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public SaleService(SaleRepository saleRepository,
            CustomerRepository customerRepository, ProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public SaleResponse create(CreateSaleRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("A sale needs at least one item");
        }
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new NotFoundException("Customer " + request.customerId()));

        Sale sale = new Sale(customer);

        for (CreateSaleRequest.Line line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Product " + line.productId()));
            if (product.getStock() < line.quantity()) {
                throw new InsufficientStockException(product.getName(), product.getStock(), line.quantity());
            }
            product.setStock(product.getStock() - line.quantity()); // dirty checking, no save()

            SaleItem item = new SaleItem(product, line.quantity());
            sale.addItem(item); // sets item.sale (the owning side: this is what fills the FK)
        }
        saleRepository.save(sale); // CascadeType.ALL persists the items too
        return SaleResponse.from(sale);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> findAll() {
        return saleRepository.findAll().stream().map(SaleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        return saleRepository.findById(id)
                .map(SaleResponse::from)
                .orElseThrow(() -> new NotFoundException("Sale " + id));
    }
}
