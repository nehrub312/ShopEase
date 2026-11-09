package com.shopease.controller;

import com.shopease.dto.ProductRequest;
import com.shopease.dto.StockRequest;
import com.shopease.entity.Product;
import com.shopease.service.ProductService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String name,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) BigDecimal minPrice,
                              @RequestParam(required = false) BigDecimal maxPrice,
                              @RequestParam(required = false) String sort) {
        return service.search(name, category, minPrice, maxPrice, sort);
    }

    @GetMapping("/search")
    public List<Product> search(@RequestParam(required = false, defaultValue = "") String name) {
        return service.search(name, null, null, null, null);
    }

    @GetMapping("/category/{category}")
    public List<Product> byCategory(@PathVariable String category) {
        return service.search(null, category, null, null, null);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        Product saved = service.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/stock")
    public Product updateStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return service.updateStock(id, request.stock());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
