package com.shopease.repository;

import com.shopease.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsByNameIgnoreCase(String name);

    List<Product> findTop4ByCategoryAndIdNot(String category, Long id);

    @Query("select coalesce(sum(p.stock), 0) from Product p")
    long totalStock();
}
