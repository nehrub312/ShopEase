package com.shopease.service;

import com.shopease.dto.ProductRequest;
import com.shopease.entity.Product;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.repository.CartItemRepository;
import com.shopease.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {
    public static final List<String> CATEGORIES = List.of(
            "Mobiles", "Laptops", "Electronics", "Fashion", "Shoes", "Home Appliances", "Accessories");

    private static final String DEFAULT_IMAGE = "/images/placeholder.svg";

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    public ProductService(ProductRepository productRepository, CartItemRepository cartItemRepository) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() { return productRepository.findAll(Sort.by("id")); }

    /** Search + filter + sort. All parameters are optional. sort: price_asc | price_desc | (anything else = newest first) */
    @Transactional(readOnly = true)
    public List<Product> search(String name, String category, BigDecimal minPrice, BigDecimal maxPrice, String sort) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (name != null && !name.isBlank())
                p.add(cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            if (category != null && !category.isBlank())
                p.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            if (minPrice != null) p.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) p.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            return cb.and(p.toArray(new Predicate[0]));
        };
        Sort order = "price_asc".equals(sort) ? Sort.by("price").ascending()
                : "price_desc".equals(sort) ? Sort.by("price").descending()
                : Sort.by("id").ascending();
        return productRepository.findAll(spec, order);
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " was not found."));
    }

    @Transactional(readOnly = true)
    public List<Product> related(Product p) {
        return productRepository.findTop4ByCategoryAndIdNot(p.getCategory(), p.getId());
    }

    public Product create(ProductRequest r) {
        Product p = new Product();
        apply(p, r);
        return productRepository.save(p);
    }

    public Product update(Long id, ProductRequest r) {
        Product p = get(id);
        apply(p, r);
        return productRepository.save(p);
    }

    public Product updateStock(Long id, int stock) {
        if (stock < 0) throw new com.shopease.exception.InvalidQuantityException("Stock cannot be negative.");
        Product p = get(id);
        p.setStock(stock);
        return productRepository.save(p);
    }

    public void delete(Long id) {
        Product p = get(id);
        cartItemRepository.deleteByProductId(p.getId()); // remove it from every cart first (FK safety)
        productRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public long count() { return productRepository.count(); }

    @Transactional(readOnly = true)
    public long totalStock() { return productRepository.totalStock(); }

    private void apply(Product p, ProductRequest r) {
        p.setName(r.name().trim());
        p.setCategory(r.category().trim());
        p.setDescription(r.description());
        p.setPrice(r.price().setScale(2, java.math.RoundingMode.HALF_UP));
        p.setStock(r.stock());
        p.setImageUrl(r.imageUrl() == null || r.imageUrl().isBlank() ? DEFAULT_IMAGE : r.imageUrl().trim());
    }
}
