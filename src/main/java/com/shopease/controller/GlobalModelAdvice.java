package com.shopease.controller;

import com.shopease.service.ProductService;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Makes the category list available to every HTML page (for the navigation bar). */
@ControllerAdvice(assignableTypes = {PageController.class, AdminController.class})
public class GlobalModelAdvice {
    @ModelAttribute("categories")
    public List<String> categories() { return ProductService.CATEGORIES; }
}
