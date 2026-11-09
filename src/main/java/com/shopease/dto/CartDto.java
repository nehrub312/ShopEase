package com.shopease.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(List<CartItemDto> items, int totalItems, BigDecimal total, boolean hasOverLimit) {}
