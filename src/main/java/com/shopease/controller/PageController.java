package com.shopease.controller;

import com.shopease.entity.Product;
import com.shopease.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {
    private final ProductService service;

    public PageController(ProductService service) { this.service = service; }

    @GetMapping("/")
    public String home(Model model) {
        List<Product> all = service.findAll();
        int size = all.size();
        // Simple, deterministic groupings of the catalogue for the homepage sections
        model.addAttribute("featured", all.stream().limit(8).toList());
        model.addAttribute("trending", all.stream().skip(Math.max(0, size - 4)).toList());
        model.addAttribute("bestSelling", service.search(null, null, null, null, "price_asc").stream().limit(4).toList());
        model.addAttribute("recommended", all.stream().filter(p -> p.getId() % 2 == 0).limit(4).toList());
        return "index";
    }

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String name,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) BigDecimal minPrice,
                           @RequestParam(required = false) BigDecimal maxPrice,
                           @RequestParam(required = false) String sort,
                           Model model) {
        model.addAttribute("products", service.search(name, category, minPrice, maxPrice, sort));
        model.addAttribute("fName", name);
        model.addAttribute("fCategory", category);
        model.addAttribute("fMin", minPrice);
        model.addAttribute("fMax", maxPrice);
        model.addAttribute("fSort", sort);
        return "products";
    }

    @GetMapping("/products/{id}")
    public String details(@PathVariable Long id, Model model) {
        Product p = service.get(id); // 404 page if not found
        model.addAttribute("product", p);
        model.addAttribute("related", service.related(p));
        return "product-details";
    }

    @GetMapping("/cart")
    public String cart() { return "cart"; }
}
