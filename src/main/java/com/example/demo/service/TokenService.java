package com.example.demo.service;

import com.example.demo.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
public class TokenService {

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Value("${jwt.access-token-expiry-minutes:30}")
    private long accessTokenExpiryMinutes;

    @Value("${jwt.refresh-token-expiry-days:7}")
    private long refreshTokenExpiryDays;

    public String generateAccessToken(Authentication auth) {
        Instant now = Instant.now();

        String scope = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(" "));

        Long customerId = null;
        Object principal = auth.getPrincipal();
        if (principal instanceof User user && user.getCustomer() != null) {
            customerId = user.getCustomer().getId();
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("demo")
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenExpiryMinutes, ChronoUnit.MINUTES))
            .subject(auth.getName())
            .claim("roles", scope)
            .claim("customerId", customerId != null ? customerId.toString() : null)
            .claim("tokenType", "access")
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateRefreshToken(Authentication auth) {
        Instant now = Instant.now();

        Long customerId = null;
        Object principal = auth.getPrincipal();
        if (principal instanceof User user && user.getCustomer() != null) {
            customerId = user.getCustomer().getId();
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("demo")
            .issuedAt(now)
            .expiresAt(now.plus(refreshTokenExpiryDays, ChronoUnit.DAYS))
            .subject(auth.getName())
            .claim("customerId", customerId != null ? customerId.toString() : null)
            .claim("tokenType", "refresh")
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateAccessTokenFromRefreshToken(String refreshToken) {
        Jwt jwt = jwtDecoder.decode(refreshToken);
        if (!"refresh".equals(jwt.getClaim("tokenType"))) {
            throw new IllegalArgumentException("Invalid token type");
        }

        String username = jwt.getSubject();
        String roles = jwt.getClaim("roles");
        String customerId = jwt.getClaim("customerId");

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("demo")
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenExpiryMinutes, ChronoUnit.MINUTES))
            .subject(username)
            .claim("roles", roles)
            .claim("customerId", customerId)
            .claim("tokenType", "access")
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String extractUserName(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return "refresh".equals(jwt.getClaim("tokenType"));
        } catch (Exception e) {
            return false;
        }
    }
}