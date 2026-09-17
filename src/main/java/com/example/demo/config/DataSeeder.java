package com.example.demo.config;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository repository;

    public DataSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        Product p1 = new Product();
        p1.setName("Wireless Keyboard");
        p1.setPrice(49.90);
        repository.save(p1);

        Product p2 = new Product();
        p2.setName("USB-C Hub");
        p2.setPrice(29.50);
        repository.save(p2);

        // Locked, so DELETE /api/products/3 demonstrates the 422 response
        Product p3 = new Product();
        p3.setName("Mechanical Mouse");
        p3.setPrice(59.00);
        p3.setLockedForOrders(true);
        repository.save(p3);
    }
}
