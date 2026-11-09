package com.shopease.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Access control: /admin/** pages and every write operation on /api/products/** require an admin session.
 * Customers can still read products (GET) and use the cart.
 */
public class AdminInterceptor implements HandlerInterceptor {
    public static final String ADMIN_ATTR = "shopeaseAdmin";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String path = req.getRequestURI();
        boolean adminPage = path.startsWith("/admin");
        boolean productWrite = path.startsWith("/api/products") && !"GET".equalsIgnoreCase(req.getMethod());
        if (!adminPage && !productWrite) return true;

        HttpSession session = req.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(ADMIN_ATTR))) return true;

        if (adminPage) {
            res.sendRedirect("/login?required");
        } else {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Admin login is required for this action.\"}");
        }
        return false;
    }
}
