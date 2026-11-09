package com.shopease.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Thymeleaf helper: ${@fmt.inr(price)} -> ₹1,05,000.00 (Indian digit grouping). */
@Component("fmt")
public class PriceFormatter {
    public String inr(BigDecimal value) {
        if (value == null) return "₹0.00";
        String[] parts = value.setScale(2, RoundingMode.HALF_UP).toPlainString().split("\\.");
        String digits = parts[0];
        boolean negative = digits.startsWith("-");
        if (negative) digits = digits.substring(1);
        String grouped;
        if (digits.length() <= 3) {
            grouped = digits;
        } else {
            String last3 = digits.substring(digits.length() - 3);
            String rest = digits.substring(0, digits.length() - 3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < rest.length(); i++) {
                if (i > 0 && (rest.length() - i) % 2 == 0) sb.append(',');
                sb.append(rest.charAt(i));
            }
            grouped = sb + "," + last3;
        }
        return (negative ? "-" : "") + "₹" + grouped + "." + parts[1];
    }
}
