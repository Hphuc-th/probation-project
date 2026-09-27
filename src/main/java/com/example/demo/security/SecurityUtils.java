package com.example.demo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
            return ud.getUsername();
        }
        return auth.getName();
    }

    public static String getCurrentRole() {
        Authentication auth = getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse(null);
    }

    public static Long getCurrentCustomerId() {
        Authentication auth = getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            String customerId = jwt.getClaim("customerId");
            if (customerId != null && !customerId.isEmpty()) {
                return Long.valueOf(customerId);
            }
        }
        return null;
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(getCurrentRole());
    }

    public static boolean isCustomer() {
        return "CUSTOMER".equals(getCurrentRole());
    }

    public static void checkOwnershipOrAdmin(Long resourceCustomerId) {
        if (isAdmin()) {
            return;
        }
        Long currentCustomerId = getCurrentCustomerId();
        if (currentCustomerId == null || !currentCustomerId.equals(resourceCustomerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: resource belongs to another customer");
        }
    }
}