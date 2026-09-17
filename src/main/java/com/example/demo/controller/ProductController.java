package com.example.demo.controller;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

// Controller stays dumb: no try/catch, no error handling. Any exception
// thrown by the service propagates up to GlobalExceptionHandler.
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "CRUD endpoints for products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List products", description = "Supports optional name filtering, sorting and pagination")
    public Page<ProductResponse> getAll(
            @RequestParam Optional<String> category,
            Pageable pageable) {
        return service.findAll(category, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single product by id")
    public ProductResponse getOne(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product", description = "Returns 409 if the name already exists")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest req) {
        ProductResponse created = service.create(req);
        URI location = URI.create("/api/products/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product", description = "Returns 404 if the product doesn't exist")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a product",
        description = "Returns 404 if missing, 422 if the product is locked for active orders")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
