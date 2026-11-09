-- ShopEase database setup (MySQL 8)
-- You normally do NOT need to run this file: the application creates the database/tables
-- (spring.jpa.hibernate.ddl-auto=update + createDatabaseIfNotExist=true) and inserts the
-- 20 sample products automatically on first start (duplicates are skipped on restart).
-- Run it manually only if you prefer to create the schema yourself:
--   mysql -u root -p < database/shopease.sql

CREATE DATABASE IF NOT EXISTS shopease CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE shopease;

CREATE TABLE IF NOT EXISTS products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150)   NOT NULL,
    category    VARCHAR(60)    NOT NULL,
    description VARCHAR(2000),
    price       DECIMAL(12,2)  NOT NULL,
    stock       INT            NOT NULL,
    image_url   VARCHAR(500)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS carts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_token VARCHAR(64) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL,
    CONSTRAINT uk_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT fk_ci_cart    FOREIGN KEY (cart_id)    REFERENCES carts(id)    ON DELETE CASCADE,
    CONSTRAINT fk_ci_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- A few extra sample rows (safe to re-run: skipped if the name already exists)
INSERT INTO products (name, category, description, price, stock, image_url)
SELECT 'Sample Wired Earphones', 'Accessories', 'Tangle-free wired earphones with mic.', 499.00, 120, '/images/placeholder.svg'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Sample Wired Earphones');

-- ============ DEMO: "existing cart quantity above 5" ============
-- 1. Open the website, add any product to the cart (creates a cart + cart_items row).
-- 2. Run the statement below to simulate old/invalid data (7 units):
--      UPDATE cart_items SET quantity = 7 ORDER BY id LIMIT 1;
-- 3. Refresh /cart: the row is highlighted red with
--      "Warning: Existing cart quantity exceeds the maximum limit of 5."
--    The + button is disabled; pressing - brings it back to 5.
