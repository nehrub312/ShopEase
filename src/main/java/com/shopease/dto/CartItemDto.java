package com.shopease.dto;

import java.math.BigDecimal;

public record CartItemDto(Long itemId, Long productId, String productName, String imageUrl,
                          BigDecimal unitPrice, int quantity, BigDecimal subtotal, int stock,
                          boolean overLimit, String warning) {}
