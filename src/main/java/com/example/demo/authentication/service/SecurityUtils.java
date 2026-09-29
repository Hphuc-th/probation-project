package com.example.demo.authentication.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String getCurrentUsername() {
    return Optional.ofNullable(getAuthentication())
            .map(Authentication::getPrincipal)
            .filter(principal -> principal instanceof Jwt)
            .map(principal -> ((Jwt) principal).getSubject())
            .orElse(null);
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
    return Optional.ofNullable(getAuthentication())
            .map(Authentication::getPrincipal)
            .filter(principal -> principal instanceof Jwt)
            .map(principal -> ((Jwt) principal).getClaimAsString("customerId"))
            .filter(id -> id != null && !id.isEmpty())
            .map(Long::valueOf) 
            .orElse(null);
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