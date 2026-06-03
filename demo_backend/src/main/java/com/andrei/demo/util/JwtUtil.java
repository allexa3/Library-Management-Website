package com.andrei.demo.util;

import com.andrei.demo.model.Person;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secretKey;

    private Date getCurrentDate() {
        return Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC));
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(Person person) {
        String role = person.getRole() != null ? person.getRole().name() : "CUSTOMER";
        return Jwts.builder()
                .subject(person.getEmail())
                .issuer("demo-spring-boot-backend")
                .issuedAt(getCurrentDate())
                .claims(Map.of(
                        "userId", person.getId().toString(), // Store as string for easy parsing
                        "role", role
                ))
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 10))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getRole(String token) {
        return getAllClaimsFromToken(token).get("role", String.class);
    }

    public String getUserId(String token) {
        return getAllClaimsFromToken(token).get("userId", String.class);
    }

    public boolean checkClaims(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            if (!"demo-spring-boot-backend".equals(claims.getIssuer())) return false;
            if (claims.getExpiration().before(getCurrentDate())) return false;
            return claims.get("userId") != null && claims.get("role") != null;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}