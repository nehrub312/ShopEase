package com.shopease.controller;

import com.shopease.dto.AddToCartRequest;
import com.shopease.dto.CartDto;
import com.shopease.dto.UpdateQuantityRequest;
import com.shopease.service.CartService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** The cart belongs to the visitor's browser session (random token stored in the HTTP session). */
@RestController
@RequestMapping("/api/cart")
public class CartController {
    private static final String TOKEN_ATTR = "shopeaseCartToken";
    private final CartService service;

    public CartController(CartService service) { this.service = service; }

    private String token(HttpSession session) {
        Object t = session.getAttribute(TOKEN_ATTR);
        if (t == null) {
            t = UUID.randomUUID().toString();
            session.setAttribute(TOKEN_ATTR, t);
        }
        return t.toString();
    }

    @GetMapping
    public CartDto get(HttpSession s) { return service.getCart(token(s)); }

    @PostMapping("/add")
    public ResponseEntity<CartDto> add(@RequestBody AddToCartRequest r, HttpSession s) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addItem(token(s), r.productId(), r.quantity()));
    }

    @PutMapping("/update/{itemId}")
    public CartDto update(@PathVariable Long itemId, @RequestBody UpdateQuantityRequest r, HttpSession s) {
        return service.updateItem(token(s), itemId, r.quantity());
    }

    @DeleteMapping("/remove/{itemId}")
    public CartDto remove(@PathVariable Long itemId, HttpSession s) { return service.removeItem(token(s), itemId); }

    @DeleteMapping("/clear")
    public CartDto clear(HttpSession s) { return service.clear(token(s)); }

    @GetMapping("/total")
    public Map<String, BigDecimal> total(HttpSession s) { return Map.of("total", service.getTotal(token(s))); }
}
