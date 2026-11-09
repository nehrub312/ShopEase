package com.shopease.controller;

import com.shopease.config.AdminInterceptor;
import com.shopease.repository.CartItemRepository;
import com.shopease.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {
    private final ProductService productService;
    private final CartItemRepository cartItemRepository;
    private final String adminPassword;

    public AdminController(ProductService productService, CartItemRepository cartItemRepository,
                           @Value("${shopease.admin.password}") String adminPassword) {
        this.productService = productService;
        this.cartItemRepository = cartItemRepository;
        this.adminPassword = adminPassword;
    }

    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String password, HttpServletRequest request) {
        boolean ok = MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8), adminPassword.getBytes(StandardCharsets.UTF_8));
        if (!ok) return "redirect:/login?error";
        request.getSession(true);
        request.changeSessionId(); // prevent session fixation
        request.getSession().setAttribute(AdminInterceptor.ADMIN_ATTR, Boolean.TRUE);
        return "redirect:/admin";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(AdminInterceptor.ADMIN_ATTR);
        return "redirect:/";
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("totalProducts", productService.count());
        model.addAttribute("totalStock", productService.totalStock());
        model.addAttribute("cartEntries", cartItemRepository.count());
        return "admin";
    }
}
