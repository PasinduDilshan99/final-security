package com.example.demo.service;

import com.example.demo.model.CustomUserDetails;
import com.example.demo.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration.time:600000}")
    private String jwtExpirationMillisRaw;

    @Value("${jwt.refresh.expiration.time:900000}")
    private String refreshJwtExpirationMillisRaw;

    @Value("${jwt.cookie.name:auth-token}")
    private String accessCookieName;

    @Value("${jwt.refresh.cookie.name:refresh-token}")
    private String refreshCookieName;

    @Value("${jwt.cookie.max-age:600}")
    private String accessCookieMaxAgeRaw;

    @Value("${jwt.refresh.cookie.max-age:900}")
    private String refreshCookieMaxAgeRaw;

    private long jwtExpirationMillis;
    private long refreshJwtExpirationMillis;
    private long accessCookieMaxAge;
    private long refreshCookieMaxAge;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.jwtExpirationMillis = parseLongSafely(jwtExpirationMillisRaw, 600_000L);
        this.refreshJwtExpirationMillis = parseLongSafely(refreshJwtExpirationMillisRaw, 900_000L);
        this.accessCookieMaxAge = parseLongSafely(accessCookieMaxAgeRaw, 600L);
        this.refreshCookieMaxAge = parseLongSafely(refreshCookieMaxAgeRaw, 900L);
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        claims.put("privileges", user.getPrivileges());
        return buildToken(claims, user.getUsername(), jwtExpirationMillis);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, user.getUsername(), refreshJwtExpirationMillis);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiryMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiryMs)))
                .signWith(getKey())
                .compact();
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims ,T> claimResolver){
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaim(token, claims -> claims.getOrDefault("type", "").toString()));
    }

    public ResponseCookie buildAccessTokenCookie(String token) {
        return ResponseCookie.from(accessCookieName, token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofSeconds(accessCookieMaxAge))
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie buildRefreshTokenCookie(String token) {
        return ResponseCookie.from(refreshCookieName, token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofSeconds(refreshCookieMaxAge))
                .sameSite("Strict")
                .build();
    }

    public Optional<String> resolveAccessToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return getCookieValue(request, accessCookieName);
    }

    public Optional<String> resolveRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, refreshCookieName);
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    public boolean canRefresh(String refreshToken, CustomUserDetails userDetails) {
        return !isTokenExpired(refreshToken)
                && validateToken(refreshToken, userDetails)
                && isRefreshToken(refreshToken);
    }

    public Instant getAccessExpiryInstant() {
        return Instant.now().plusMillis(jwtExpirationMillis);
    }

    public Instant getRefreshExpiryInstant() {
        return Instant.now().plusMillis(refreshJwtExpirationMillis);
    }

    private long parseLongSafely(String value, long defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        String digitsOnly = value.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digitsOnly)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(digitsOnly);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
