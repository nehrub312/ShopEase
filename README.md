# ShopEase – Online Shopping Cart System

Java 17 · Spring Boot 3 · Spring MVC · Spring Data JPA/Hibernate · MySQL 8 · Thymeleaf · Bootstrap 5 · Maven

A single Spring Boot application: server-rendered pages (Thymeleaf) plus REST APIs used by JavaScript `fetch` for instant cart updates.

## 1. Software to install (Windows)
| Software | Version | Link |
|---|---|---|
| JDK | 17 or newer | https://adoptium.net (tick *Add to PATH* and *Set JAVA_HOME*) |
| Apache Maven | 3.9+ | https://maven.apache.org/download.cgi – unzip, add `bin` to PATH |
| MySQL Community Server (optional) | 8.x | only needed for the `mysql` profile |
| VS Code + *Extension Pack for Java* + *Spring Boot Extension Pack* | latest | https://code.visualstudio.com |
| Google Chrome | latest | |

Check in a new Command Prompt: `java -version`, `mvn -version`, `mysql --version`.

## 2. Database (no setup needed)
By default ShopEase uses an **embedded H2 database** (MySQL-compatible mode) stored in the `data/` folder. Tables `products`, `carts`, `cart_items` and the **20 sample products** are created automatically on first start; restarts never duplicate them.

### Optional: use real MySQL 8 (database `shopease`)
```
set DB_PASSWORD=your_mysql_password
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```
Variables: `DB_USERNAME` (default root), `DB_PASSWORD` (default root), `DB_HOST`, `DB_PORT`, `DB_NAME`. Manual schema: `mysql -u root -p < database/shopease.sql`. MySQL must be installed and running.

## 4. Run the project
**Single command:** `mvn spring-boot:run`  (or double-click `RUN_PROJECT.bat`).

**VS Code:** File → Open Folder → `ShopEase`, open a terminal (Ctrl+`), then:
```
mvn spring-boot:run
```
**Maven commands**
```
mvn clean test        # run automated tests (uses in-memory H2, no MySQL needed)
mvn clean package     # build target/shopease-1.0.0.jar
java -jar target\shopease-1.0.0.jar
```
Open **http://localhost:8080** in Chrome. (Internet access is needed for Bootstrap CDN and product photos.)

## 5. Using the site
* `/` home · `/products` search/filter/sort · `/products/{id}` details · `/cart` cart
* **Admin dashboard:** http://localhost:8080/admin → *Login / Register* link → *Admin Login*. Default password **admin123** (change via `ADMIN_PASSWORD`).
  Customers do not need an account; their cart is tied to their browser session. Product write APIs (POST/PUT/PATCH/DELETE `/api/products/**`) and `/admin` are admin-only; GET product APIs and all cart APIs are public.

## 6. Quantity rule (max 5 per product)
* Backend (`CartService`) rejects 0, negative and >5 with **"Maximum quantity allowed is 5 per product."**; the frontend checks too, but is never trusted.
* Adding to an item already in the cart counts the combined quantity.
* Existing entry above 5 is **never silently changed**: it is highlighted red with **"Warning: Existing cart quantity exceeds the maximum limit of 5."**, increases are blocked, and it can be reduced (the `-` button drops it straight to 5).
* **Demo:** add a product to the cart, then run in MySQL `UPDATE cart_items SET quantity = 7 ORDER BY id LIMIT 1;` and refresh `/cart`.
* Prices come only from the database; totals use `BigDecimal` on the server (₹1,500×2 + ₹2,000×1 = ₹5,000).

## 7. REST API
```
GET    /api/products?name=&category=&minPrice=&maxPrice=&sort=price_asc|price_desc
GET    /api/products/{id}            GET /api/products/search?name=     GET /api/products/category/{category}
POST   /api/products   PUT /api/products/{id}   PATCH /api/products/{id}/stock   DELETE /api/products/{id}   (admin)
GET    /api/cart        POST /api/cart/add {productId, quantity}
PUT    /api/cart/update/{itemId} {quantity}     DELETE /api/cart/remove/{itemId}
DELETE /api/cart/clear  GET /api/cart/total
```
Errors return JSON `{status, error, message, timestamp}` – 400 invalid quantity/validation, 401 not admin, 404 not found, 409 insufficient stock, 503 database problem. No stack traces are shown.

## 8. Tests (`src/test/java/com/shopease`)
`CartServiceTest` (add, update, >5 rejection, zero/negative, existing-quantity warning, remove, totals, stock) · `ProductServiceTest` (search/filter/sort, create/update/stock/delete) · `ApiTest` (HTTP status codes, admin protection, pages render). Tests use an in-memory H2 database (test scope only).

## 9. Troubleshooting
| Problem | Fix |
|---|---|
| `'java' / 'mvn' is not recognized` | Install it and add to PATH; open a **new** terminal. |
| `Communications link failure` / `Connection refused` | MySQL is not running: Win+R → `services.msc` → start **MySQL80**. |
| `Access denied for user 'root'` | Wrong password → set `DB_PASSWORD`. |
| `Port 8080 was already in use` | Close the other app or add `server.port=8081` to `application.properties`. |
| `release version 17 not supported` | Your JDK is older than 17. |
| Product images blank | No internet or image site down – the app shows a fallback image; set your own Image URL in the admin page. |
| Page shows "Something went wrong" | Check the terminal log for the real error. |
| Maven cannot download dependencies | Check internet/proxy; retry `mvn clean package`. |

## 10. Project structure
```
ShopEase/ pom.xml · README.md · RUN_PROJECT.bat · database/shopease.sql
src/main/java/com/shopease/{controller,service,repository,entity,dto,exception,config}
src/main/resources/{application.properties, templates/*.html, static/{css,js,images}}
src/test/java/com/shopease
```
