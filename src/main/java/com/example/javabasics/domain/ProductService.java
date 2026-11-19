package com.example.javabasics.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.javabasics.web.NotFoundException;
import com.example.javabasics.web.ProductPatchRequest;
import com.example.javabasics.web.ProductReplaceRequest;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * Note: there is NO repository.save() here.
     * The entity is MANAGED inside this transaction, so at commit Hibernate compares it
     * with the snapshot taken when it was loaded (dirty checking) and issues the UPDATE.
     */
    @Transactional
    public Product update(Long id, ProductPatchRequest request) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product " + id));
        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.stock() != null) {
            product.setStock(request.stock());
        }
        return product;
    }

    /**
     * NAIVE on purpose. The request is turned into an entity that has an id, so save()
     * calls em.merge(): every column is overwritten with the incoming values
     * (a missing "stock" becomes 0). With an unknown id it fails with a confusing
     * "Row was already updated or deleted" error.
     * Compare with update(): load the managed entity and change only what was sent.
     */
    @Transactional
    public Product replaceNaive(Long id, ProductReplaceRequest request) {
        return repository.save(request.toEntity(id));
    }
}
