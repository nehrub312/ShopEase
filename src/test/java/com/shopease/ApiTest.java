package com.shopease;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.shopease.config.AdminInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {
    @Autowired MockMvc mvc;

    @Test
    void listsProducts() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(20)));
        mvc.perform(get("/api/products/search").param("name", "watch")).andExpect(status().isOk());
        mvc.perform(get("/api/products/category/Mobiles")).andExpect(status().isOk());
    }

    @Test
    void unknownProductReturns404WithFriendlyMessage() throws Exception {
        mvc.perform(get("/api/products/9999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with id 9999999 was not found."));
    }

    @Test
    void addToCartRejectsQuantitySixWith400() throws Exception {
        mvc.perform(post("/api/cart/add").contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maximum quantity allowed is 5 per product."));
    }

    @Test
    void cartFlowAddUpdateTotalRemoveClear() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/cart/add").session(session).contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalItems").value(2));
        mvc.perform(get("/api/cart/total").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.total").exists());
        mvc.perform(get("/api/cart").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(delete("/api/cart/clear").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(delete("/api/cart/clear").session(session)).andExpect(status().isBadRequest());
    }

    @Test
    void productWritesRequireAdminSession() throws Exception {
        String body = "{\"name\":\"API Product\",\"category\":\"Accessories\",\"description\":\"x\",\"price\":99.00,\"stock\":3,\"imageUrl\":\"\"}";
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());

        MockHttpSession admin = new MockHttpSession();
        admin.setAttribute(AdminInterceptor.ADMIN_ATTR, Boolean.TRUE);
        mvc.perform(post("/api/products").session(admin).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/products").session(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Bad\",\"category\":\"Accessories\",\"price\":-5,\"stock\":1}")).andExpect(status().isBadRequest());
    }

    @Test
    void adminPageRedirectsToLoginWhenNotSignedIn() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    void publicPagesRender() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/products")).andExpect(status().isOk());
        mvc.perform(get("/products/1")).andExpect(status().isOk());
        mvc.perform(get("/cart")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
    }
}
