package com.example.demo.service;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.DuplicateResourceException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

// The service owns business logic and throws domain-specific exceptions.
// It knows nothing about HTTP status codes - that mapping lives only in
// GlobalExceptionHandler.
@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Page<ProductResponse> findAll(Optional<String> category, Pageable pageable) {
        Page<Product> page = category
            .map(c -> repository.findByNameContainingIgnoreCase(c, pageable))
            .orElseGet(() -> repository.findAll(pageable));
        return page.map(this::toResponse);
    }

    public ProductResponse findById(Long id) {
        Product p = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return toResponse(p);
    }

    public ProductResponse create(ProductRequest req) {
        if (repository.existsByName(req.name())) {
            throw new DuplicateResourceException(
                "A product named '" + req.name() + "' already exists");
        }
        Product p = new Product();
        p.setName(req.name());
        p.setPrice(req.price());
        return toResponse(repository.save(p));
    }

    public ProductResponse update(Long id, ProductRequest req) {
        Product p = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        p.setName(req.name());
        p.setPrice(req.price());
        return toResponse(repository.save(p));
    }

    public void delete(Long id) {
        Product p = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (p.isLockedForOrders()) {
            throw new BusinessRuleException("Cannot delete a product with active orders");
        }
        repository.delete(p);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(p.getId(), p.getName(), p.getPrice());
    }
}
