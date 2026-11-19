package com.example.javabasics.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.javabasics.demo.Tx;
import com.example.javabasics.domain.Customer;
import com.example.javabasics.domain.CustomerRepository;
import com.example.javabasics.domain.Product;
import com.example.javabasics.domain.ProductRepository;
import com.example.javabasics.domain.Sale;
import com.example.javabasics.domain.SaleItem;
import com.example.javabasics.domain.SaleRepository;

/** Creates sample data so the demos have something to work with. Parameters control the size. */
@RestController
@RequestMapping("/admin")
public class SeedController {

    private final Tx tx;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;

    public SeedController(Tx tx, CustomerRepository customerRepository, ProductRepository productRepository,
            SaleRepository saleRepository) {
        this.tx = tx;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
    }

    @PostMapping("/seed")
    public Map<String, Object> seed(
            @RequestParam(defaultValue = "5") int customers,
            @RequestParam(defaultValue = "5") int products,
            @RequestParam(defaultValue = "3") int salesPerCustomer,
            @RequestParam(defaultValue = "2") int itemsPerSale) {

        String run = Long.toString(System.currentTimeMillis(), 36);
        Map<String, Object> created = new LinkedHashMap<>();

        tx.runVoid(() -> {
            List<Product> newProducts = new ArrayList<>();
            for (int i = 1; i <= products; i++) {
                newProducts.add(productRepository.save(new Product("Product " + run + "-" + i, 1000)));
            }
            int sales = 0;
            int items = 0;
            for (int c = 1; c <= customers; c++) {
                Customer customer = customerRepository.save(
                        new Customer("Customer " + run + "-" + c, "customer-" + run + "-" + c + "@example.com"));
                for (int s = 0; s < salesPerCustomer; s++) {
                    Sale sale = new Sale(customer);
                    sales++;
                    for (int k = 0; k < itemsPerSale && !newProducts.isEmpty(); k++) {
                        Product product = newProducts.get((c + s + k) % newProducts.size());
                        sale.addItem(new SaleItem(product, 1 + (k % 3)));
                        items++;
                    }
                    saleRepository.save(sale); // cascade saves the items
                }
            }
            created.put("customers", customers);
            created.put("products", products);
            created.put("sales", sales);
            created.put("saleItems", items);
        });
        return created;
    }
}
