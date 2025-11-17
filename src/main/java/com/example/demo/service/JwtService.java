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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
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
    private long jwtExpirationMillis;

    @Value("${jwt.refresh.expiration.time:900000}")
    private long refreshJwtExpirationMillis;

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
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
    public Optional<String> resolveAccessToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }
        return Optional.empty();
    }

    public Optional<String> resolveRefreshToken(HttpServletRequest request) {
        String header = request.getHeader("X-Refresh-Token");
        if (StringUtils.hasText(header)) {
            return Optional.of(header.trim());
        }
        String altHeader = request.getHeader("Refresh-Token");
        if (StringUtils.hasText(altHeader)) {
            return Optional.of(altHeader.trim());
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
}
