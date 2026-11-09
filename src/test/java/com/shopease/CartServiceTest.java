package com.shopease;

import static org.junit.jupiter.api.Assertions.*;

import com.shopease.dto.CartDto;
import com.shopease.dto.CartItemDto;
import com.shopease.entity.Cart;
import com.shopease.entity.CartItem;
import com.shopease.entity.Product;
import com.shopease.exception.*;
import com.shopease.repository.CartItemRepository;
import com.shopease.repository.CartRepository;
import com.shopease.repository.ProductRepository;
import com.shopease.service.CartService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc // same config as ApiTest so Spring reuses one cached context (and one H2 database)
@Transactional
class CartServiceTest {
    private static final String TOKEN = "test-token";

    @Autowired CartService cartService;
    @Autowired ProductRepository productRepository;
    @Autowired CartRepository cartRepository;
    @Autowired CartItemRepository cartItemRepository;

    private Product product(String name, String price, int stock) {
        return productRepository.save(new Product(name, "Electronics", "test", new BigDecimal(price), stock, "/images/placeholder.svg"));
    }

    // 1. Adding products to cart
    @Test
    void addsProductToCart() {
        Product p = product("T-Headphones", "1500.00", 10);
        CartDto cart = cartService.addItem(TOKEN, p.getId(), 2);
        assertEquals(1, cart.items().size());
        assertEquals(2, cart.items().get(0).quantity());
        assertEquals(2, cart.totalItems());
    }

    @Test
    void addingSameProductTwiceAccumulatesQuantity() {
        Product p = product("T-Cable", "100.00", 10);
        cartService.addItem(TOKEN, p.getId(), 2);
        CartDto cart = cartService.addItem(TOKEN, p.getId(), 1);
        assertEquals(1, cart.items().size());
        assertEquals(3, cart.items().get(0).quantity());
    }

    @Test
    void unknownProductIsRejected() {
        assertThrows(ResourceNotFoundException.class, () -> cartService.addItem(TOKEN, 999999L, 1));
    }

    // 2. Updating quantities
    @Test
    void updatesQuantityUpAndDown() {
        Product p = product("T-Mouse", "500.00", 10);
        Long itemId = cartService.addItem(TOKEN, p.getId(), 1).items().get(0).itemId();
        assertEquals(4, cartService.updateItem(TOKEN, itemId, 4).items().get(0).quantity());
        assertEquals(2, cartService.updateItem(TOKEN, itemId, 2).items().get(0).quantity());
    }

    // 3. Rejecting quantity above five (+ zero / negative)
    @Test
    void rejectsQuantityAboveFiveOnAdd() {
        Product p = product("T-Pen", "10.00", 100);
        InvalidQuantityException ex = assertThrows(InvalidQuantityException.class, () -> cartService.addItem(TOKEN, p.getId(), 6));
        assertEquals("Maximum quantity allowed is 5 per product.", ex.getMessage());
    }

    @Test
    void rejectsCumulativeQuantityAboveFive() {
        Product p = product("T-Book", "10.00", 100);
        cartService.addItem(TOKEN, p.getId(), 3);
        assertThrows(InvalidQuantityException.class, () -> cartService.addItem(TOKEN, p.getId(), 3));
    }

    @Test
    void rejectsQuantityAboveFiveOnUpdate() {
        Product p = product("T-Bag", "10.00", 100);
        Long itemId = cartService.addItem(TOKEN, p.getId(), 1).items().get(0).itemId();
        InvalidQuantityException ex = assertThrows(InvalidQuantityException.class, () -> cartService.updateItem(TOKEN, itemId, 6));
        assertEquals(CartService.MAX_MESSAGE, ex.getMessage());
    }

    @Test
    void rejectsZeroAndNegativeQuantities() {
        Product p = product("T-Lamp", "10.00", 100);
        assertThrows(InvalidQuantityException.class, () -> cartService.addItem(TOKEN, p.getId(), 0));
        assertThrows(InvalidQuantityException.class, () -> cartService.addItem(TOKEN, p.getId(), -2));
        assertThrows(InvalidQuantityException.class, () -> cartService.addItem(TOKEN, p.getId(), null));
        Long itemId = cartService.addItem(TOKEN, p.getId(), 1).items().get(0).itemId();
        assertThrows(InvalidQuantityException.class, () -> cartService.updateItem(TOKEN, itemId, 0));
        assertThrows(InvalidQuantityException.class, () -> cartService.updateItem(TOKEN, itemId, -1));
    }

    // 4. Existing quantity warning (legacy row above the limit)
    @Test
    void existingQuantityAboveFiveIsFlaggedAndCanOnlyBeReduced() {
        Product p = product("T-Legacy", "100.00", 50);
        cartService.getCart(TOKEN); // creates the cart
        Cart cart = cartRepository.findByToken(TOKEN).orElseThrow();
        CartItem legacy = cartItemRepository.saveAndFlush(new CartItem(cart, p, 7)); // simulates old/invalid data
        cart.getItems().add(legacy);

        CartDto dto = cartService.getCart(TOKEN);
        CartItemDto item = dto.items().get(0);
        assertEquals(7, item.quantity(), "existing quantity must NOT be silently changed");
        assertTrue(item.overLimit());
        assertEquals("Warning: Existing cart quantity exceeds the maximum limit of 5.", item.warning());
        assertTrue(dto.hasOverLimit());

        // further increases are blocked
        assertThrows(InvalidQuantityException.class, () -> cartService.updateItem(TOKEN, item.itemId(), 8));
        InvalidQuantityException add = assertThrows(InvalidQuantityException.class, () -> cartService.addItem(TOKEN, p.getId(), 1));
        assertEquals(CartService.OVER_LIMIT_MESSAGE, add.getMessage());

        // decreasing to 5 or below is allowed and clears the warning
        CartDto fixed = cartService.updateItem(TOKEN, item.itemId(), 5);
        assertEquals(5, fixed.items().get(0).quantity());
        assertFalse(fixed.hasOverLimit());
    }

    // 5. Removing cart items
    @Test
    void removesItemAndUpdatesTotal() {
        Product a = product("T-A", "1500.00", 10);
        Product b = product("T-B", "2000.00", 10);
        cartService.addItem(TOKEN, a.getId(), 2);
        CartDto cart = cartService.addItem(TOKEN, b.getId(), 1);
        Long idOfA = cart.items().stream().filter(i -> i.productId().equals(a.getId())).findFirst().orElseThrow().itemId();
        CartDto after = cartService.removeItem(TOKEN, idOfA);
        assertEquals(1, after.items().size());
        assertEquals(0, new BigDecimal("2000.00").compareTo(after.total()));
        assertThrows(ResourceNotFoundException.class, () -> cartService.removeItem(TOKEN, idOfA));
    }

    @Test
    void clearsCartAndRejectsClearingEmptyCart() {
        Product p = product("T-Clear", "10.00", 10);
        cartService.addItem(TOKEN, p.getId(), 1);
        assertTrue(cartService.clear(TOKEN).items().isEmpty());
        assertThrows(EmptyCartException.class, () -> cartService.clear(TOKEN));
    }

    // 6. Calculating cart totals (example from the problem statement)
    @Test
    void calculatesSubtotalsAndTotalWithBigDecimal() {
        Product headphones = product("T-Wireless Headphones", "1500.00", 10);
        Product watch = product("T-Smart Watch", "2000.00", 10);
        cartService.addItem(TOKEN, headphones.getId(), 2);
        CartDto cart = cartService.addItem(TOKEN, watch.getId(), 1);
        for (CartItemDto i : cart.items()) {
            BigDecimal expected = i.unitPrice().multiply(BigDecimal.valueOf(i.quantity()));
            assertEquals(0, expected.compareTo(i.subtotal()));
        }
        assertEquals(0, new BigDecimal("3000.00").compareTo(cart.items().get(0).subtotal()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(cart.total()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(cartService.getTotal(TOKEN)));
    }

    // 7. Stock validation
    @Test
    void rejectsQuantityAboveAvailableStock() {
        Product p = product("T-LowStock", "10.00", 3);
        InsufficientStockException ex = assertThrows(InsufficientStockException.class, () -> cartService.addItem(TOKEN, p.getId(), 4));
        assertTrue(ex.getMessage().contains("3"));
        Long itemId = cartService.addItem(TOKEN, p.getId(), 3).items().get(0).itemId();
        assertThrows(InsufficientStockException.class, () -> cartService.updateItem(TOKEN, itemId, 4));
    }

    @Test
    void outOfStockProductCannotBeAdded() {
        Product p = product("T-SoldOut", "10.00", 0);
        assertThrows(InsufficientStockException.class, () -> cartService.addItem(TOKEN, p.getId(), 1));
    }
}
