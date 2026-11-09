package com.shopease.service;

import com.shopease.dto.CartDto;
import com.shopease.dto.CartItemDto;
import com.shopease.entity.Cart;
import com.shopease.entity.CartItem;
import com.shopease.entity.Product;
import com.shopease.exception.*;
import com.shopease.repository.CartRepository;
import com.shopease.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * All cart rules live here (backend validation - the frontend is never trusted):
 *  - quantity must be 1..5 per product
 *  - an existing entry above 5 is flagged, never silently changed, cannot be increased, only reduced
 *  - quantity cannot exceed available stock
 *  - prices always come from the database; totals use BigDecimal
 */
@Service
@Transactional
public class CartService {
    public static final int MAX_QUANTITY = 5;
    public static final String MAX_MESSAGE = "Maximum quantity allowed is 5 per product.";
    public static final String OVER_LIMIT_MESSAGE = "Warning: Existing cart quantity exceeds the maximum limit of 5.";

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public CartDto getCart(String token) { return toDto(cart(token)); }

    public BigDecimal getTotal(String token) { return toDto(cart(token)).total(); }

    public CartDto addItem(String token, Long productId, Integer quantity) {
        if (productId == null) throw new InvalidQuantityException("Please choose a product to add.");
        int qty = validateQuantity(quantity);
        Cart cart = cart(token);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + productId + " was not found."));
        CartItem existing = find(cart, product.getId());

        int newQty = qty;
        if (existing != null) {
            if (existing.getQuantity() > MAX_QUANTITY) throw new InvalidQuantityException(OVER_LIMIT_MESSAGE);
            newQty = existing.getQuantity() + qty;
        }
        if (newQty > MAX_QUANTITY) throw new InvalidQuantityException(MAX_MESSAGE);
        checkStock(product, newQty);

        if (existing == null) {
            CartItem item = new CartItem(cart, product, newQty);
            cart.getItems().add(item);
        } else {
            existing.setQuantity(newQty);
        }
        return save(cart);
    }

    public CartDto updateItem(String token, Long itemId, Integer quantity) {
        if (quantity == null || quantity <= 0)
            throw new InvalidQuantityException("Quantity must be at least 1. Use Remove to delete the item.");
        Cart cart = cart(token);
        CartItem item = findById(cart, itemId);
        int current = item.getQuantity();

        if (quantity > MAX_QUANTITY) {
            throw new InvalidQuantityException(current > MAX_QUANTITY ? OVER_LIMIT_MESSAGE : MAX_MESSAGE);
        }
        if (quantity > current) checkStock(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return save(cart);
    }

    public CartDto removeItem(String token, Long itemId) {
        Cart cart = cart(token);
        CartItem item = findById(cart, itemId);
        cart.getItems().remove(item);
        return save(cart);
    }

    public CartDto clear(String token) {
        Cart cart = cart(token);
        if (cart.getItems().isEmpty()) throw new EmptyCartException("Your cart is already empty.");
        cart.getItems().clear();
        return save(cart);
    }

    // ---------------- helpers ----------------

    private int validateQuantity(Integer q) {
        if (q == null || q <= 0) throw new InvalidQuantityException("Quantity must be at least 1.");
        if (q > MAX_QUANTITY) throw new InvalidQuantityException(MAX_MESSAGE);
        return q;
    }

    private void checkStock(Product product, int wanted) {
        if (wanted > product.getStock()) {
            throw new InsufficientStockException(product.getStock() <= 0
                    ? product.getName() + " is out of stock."
                    : "Only " + product.getStock() + " unit(s) of " + product.getName() + " available in stock.");
        }
    }

    private Cart cart(String token) {
        return cartRepository.findByToken(token).orElseGet(() -> {
            Cart c = new Cart();
            c.setToken(token);
            return cartRepository.save(c);
        });
    }

    private CartItem find(Cart cart, Long productId) {
        return cart.getItems().stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst().orElse(null);
    }

    private CartItem findById(Cart cart, Long itemId) {
        return cart.getItems().stream().filter(i -> i.getId() != null && i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item " + itemId + " was not found."));
    }

    private CartDto save(Cart cart) {
        cartRepository.saveAndFlush(cart);
        return toDto(cart);
    }

    private CartDto toDto(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toItemDto).toList();
        BigDecimal total = items.stream().map(CartItemDto::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        int count = items.stream().mapToInt(CartItemDto::quantity).sum();
        boolean over = items.stream().anyMatch(CartItemDto::overLimit);
        return new CartDto(items, count, total, over);
    }

    private CartItemDto toItemDto(CartItem i) {
        Product p = i.getProduct();
        BigDecimal subtotal = p.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())).setScale(2, java.math.RoundingMode.HALF_UP);
        boolean over = i.getQuantity() > MAX_QUANTITY;
        return new CartItemDto(i.getId(), p.getId(), p.getName(), p.getImageUrl(), p.getPrice(), i.getQuantity(),
                subtotal, p.getStock(), over, over ? OVER_LIMIT_MESSAGE : null);
    }
}
