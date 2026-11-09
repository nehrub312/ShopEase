package com.shopease.repository;

import com.shopease.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteByProductId(Long productId);
}
