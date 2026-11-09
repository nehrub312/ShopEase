package com.shopease;

import static org.junit.jupiter.api.Assertions.*;

import com.shopease.dto.ProductRequest;
import com.shopease.entity.Product;
import com.shopease.exception.ResourceNotFoundException;
import com.shopease.service.CartService;
import com.shopease.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc // same config as ApiTest so Spring reuses one cached context (and one H2 database)
@Transactional
class ProductServiceTest {
    @Autowired ProductService productService;
    @Autowired CartService cartService;
    @Autowired jakarta.persistence.EntityManager em;

    @Test
    void sampleDataHasAtLeastTwentyProducts() {
        assertTrue(productService.count() >= 20);
    }

    // 8. Product search
    @Test
    void searchesByNameCaseInsensitive() {
        List<Product> r = productService.search("laptop", null, null, null, null);
        assertFalse(r.isEmpty());
        assertTrue(r.stream().allMatch(p -> p.getName().toLowerCase().contains("laptop")));
    }

    @Test
    void filtersByCategoryAndPriceAndSorts() {
        List<Product> mobiles = productService.search(null, "Mobiles", null, null, null);
        assertFalse(mobiles.isEmpty());
        assertTrue(mobiles.stream().allMatch(p -> p.getCategory().equals("Mobiles")));

        List<Product> ranged = productService.search(null, null, new BigDecimal("1000"), new BigDecimal("3000"), "price_asc");
        assertFalse(ranged.isEmpty());
        for (int i = 0; i < ranged.size(); i++) {
            assertTrue(ranged.get(i).getPrice().compareTo(new BigDecimal("1000")) >= 0);
            assertTrue(ranged.get(i).getPrice().compareTo(new BigDecimal("3000")) <= 0);
            if (i > 0) assertTrue(ranged.get(i - 1).getPrice().compareTo(ranged.get(i).getPrice()) <= 0);
        }
        List<Product> desc = productService.search(null, null, null, null, "price_desc");
        assertTrue(desc.get(0).getPrice().compareTo(desc.get(desc.size() - 1).getPrice()) >= 0);
    }

    @Test
    void searchWithNoMatchReturnsEmptyList() {
        assertTrue(productService.search("zzzz-no-such-product", null, null, null, null).isEmpty());
    }

    // 9. Product management
    @Test
    void createUpdateStockAndDeleteProduct() {
        Product created = productService.create(new ProductRequest("Test Gadget", "Electronics", "desc", new BigDecimal("999.50"), 7, null));
        assertNotNull(created.getId());
        assertEquals("/images/placeholder.svg", created.getImageUrl());

        Product updated = productService.update(created.getId(), new ProductRequest("Test Gadget 2", "Accessories", "d2", new BigDecimal("1200"), 9, "http://x/y.png"));
        assertEquals("Test Gadget 2", updated.getName());
        assertEquals(9, updated.getStock());

        assertEquals(20, productService.updateStock(created.getId(), 20).getStock());
        assertThrows(RuntimeException.class, () -> productService.updateStock(created.getId(), -1));

        productService.delete(created.getId());
        assertThrows(ResourceNotFoundException.class, () -> productService.get(created.getId()));
    }

    @Test
    void deletingProductAlsoRemovesItFromCarts() {
        Product p = productService.create(new ProductRequest("Cart Gadget", "Electronics", "d", new BigDecimal("10"), 5, null));
        cartService.addItem("del-token", p.getId(), 1);
        em.flush();
        em.clear();
        productService.delete(p.getId());
        em.flush();
        em.clear();
        assertTrue(cartService.getCart("del-token").items().isEmpty());
    }
}
