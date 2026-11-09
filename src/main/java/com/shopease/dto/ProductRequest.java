package com.shopease.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Product name is required.") @Size(max = 150, message = "Product name is too long.") String name,
        @NotBlank(message = "Category is required.") String category,
        @Size(max = 2000, message = "Description is too long.") String description,
        @NotNull(message = "Price is required.") @DecimalMin(value = "0.01", message = "Price must be greater than zero.") BigDecimal price,
        @NotNull(message = "Stock is required.") @Min(value = 0, message = "Stock cannot be negative.") Integer stock,
        @Size(max = 500, message = "Image URL is too long.") String imageUrl) {}
