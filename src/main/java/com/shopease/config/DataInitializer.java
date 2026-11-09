package com.shopease.config;

import com.shopease.entity.Product;
import com.shopease.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Inserts the 20 sample products on first start. Products that already exist (by name) are skipped, so restarts never duplicate data. */
@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final ProductRepository repo;
    private int n = 0;

    public DataInitializer(ProductRepository repo) { this.repo = repo; }

    private Product p(String name, String cat, String desc, String price, int stock, String keywords) {
        n++;
        // Photo from loremflickr by keyword (locked so the same photo is always returned). The UI falls back to /images/placeholder.svg if it fails to load.
        String img = "https://loremflickr.com/600/600/" + keywords + "?lock=" + (100 + n);
        return new Product(name, cat, desc, new BigDecimal(price), stock, img);
    }

    @Override
    public void run(String... args) {
        List<Product> all = new ArrayList<>();
        all.add(p("Nova X5 5G Smartphone (8GB/128GB)", "Mobiles", "6.67\" AMOLED 120Hz display, 50MP triple camera, 5000mAh battery with 67W fast charging and 5G connectivity.", "24999.00", 40, "smartphone"));
        all.add(p("Aurora S10 Pro Smartphone (12GB/256GB)", "Mobiles", "Flagship performance with a 200MP camera, Snapdragon-class processor, 6.8\" QHD+ display and wireless charging.", "42999.00", 25, "smartphone,phone"));
        all.add(p("Spark Lite 4G Smartphone (4GB/64GB)", "Mobiles", "Reliable everyday phone with a 6.5\" HD+ display, 13MP camera and a two-day battery.", "8999.00", 60, "mobile,phone"));
        all.add(p("ProBook 14 Laptop - Core i5, 16GB, 512GB SSD", "Laptops", "14\" Full HD anti-glare display, backlit keyboard, Windows 11 and 10-hour battery life for work and study.", "54999.00", 15, "laptop"));
        all.add(p("UltraSlim Air 13 Laptop - Core i7, 16GB, 1TB SSD", "Laptops", "Only 1.1 kg with a 13.3\" 2.8K OLED display, fingerprint reader and all-day battery.", "69999.00", 10, "laptop,computer"));
        all.add(p("GameForce 15 Gaming Laptop - RTX Graphics", "Laptops", "15.6\" 144Hz display, dedicated RTX graphics, RGB keyboard and advanced cooling for smooth gaming.", "89999.00", 8, "gaming,laptop"));
        all.add(p("Wireless Headphones", "Electronics", "Bluetooth 5.3 over-ear headphones with active noise cancellation and 40-hour playback.", "1500.00", 80, "headphones"));
        all.add(p("Smart Watch", "Electronics", "1.8\" AMOLED display, heart-rate and SpO2 tracking, GPS, 100+ sports modes and 7-day battery.", "2000.00", 50, "smartwatch"));
        all.add(p("Portable Bluetooth Speaker", "Electronics", "Waterproof (IPX7) speaker with deep bass, 20-hour playtime and built-in microphone.", "2499.00", 45, "speaker"));
        all.add(p("4K Action Camera", "Electronics", "Ultra-HD 4K/60fps video, image stabilisation, waterproof housing and Wi-Fi sharing.", "6999.00", 20, "camera"));
        all.add(p("Men's Casual Cotton Shirt", "Fashion", "Breathable 100% cotton slim-fit shirt, perfect for office and weekend wear.", "899.00", 100, "shirt"));
        all.add(p("Women's Floral Summer Dress", "Fashion", "Lightweight knee-length dress with a flattering fit and soft, skin-friendly fabric.", "1299.00", 70, "dress,fashion"));
        all.add(p("Slim Fit Stretch Denim Jeans", "Fashion", "Comfort-stretch denim with a modern slim fit and durable stitching.", "1499.00", 90, "jeans"));
        all.add(p("Runner Pro Sports Shoes", "Shoes", "Cushioned, breathable mesh running shoes with a grippy anti-slip sole.", "2799.00", 55, "sneakers"));
        all.add(p("Classic Leather Formal Shoes", "Shoes", "Genuine leather Oxford shoes with a padded insole for all-day comfort.", "3299.00", 35, "shoes,leather"));
        all.add(p("Comfort Walk Sandals", "Shoes", "Lightweight, flexible sandals with an orthopaedic footbed for daily use.", "799.00", 75, "sandals"));
        all.add(p("Digital Air Fryer 5.5L", "Home Appliances", "Cook crispy food with up to 90% less oil. 8 preset menus, digital touch panel and non-stick basket.", "4999.00", 30, "airfryer,kitchen"));
        all.add(p("Mixer Grinder 750W with 3 Jars", "Home Appliances", "Powerful 750W motor, stainless-steel blades and three leak-proof jars for wet and dry grinding.", "3499.00", 40, "blender,kitchen"));
        all.add(p("Anti-Theft Laptop Backpack", "Accessories", "Water-resistant 30L backpack with USB charging port, padded laptop sleeve and hidden zip pockets.", "1199.00", 85, "backpack"));
        all.add(p("Fast Charging Power Bank 20000mAh", "Accessories", "22.5W fast charging, dual USB-A and USB-C ports, LED display and compact design.", "1999.00", 65, "powerbank,charger"));

        int added = 0;
        for (Product pr : all) {
            if (!repo.existsByNameIgnoreCase(pr.getName())) { repo.save(pr); added++; }
        }
        log.info("ShopEase sample data: {} product(s) inserted, {} total in database.", added, repo.count());
    }
}
