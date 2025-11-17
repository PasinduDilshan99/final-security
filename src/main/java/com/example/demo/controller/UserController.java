package com.example.demo.controller;

import com.example.demo.model.TokenResponse;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/v0/user")
public class UserController {

    private final UserService userService;

    @Value("${jwt.cookie.name}")
    private String jwtCookieName;

    @Value("${jwt.refresh.cookie.name}")
    private String refreshCookieName;

    @Value("${jwt.cookie.secure}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.http-only}")
    private boolean cookieHttpOnly;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(path = "/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        userService.signup(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping(path = "/login")
    public ResponseEntity<TokenResponse> login(@RequestBody User user, HttpServletResponse response) {
        TokenResponse tokenResponse = userService.login(user);

        // Set access token cookie
        Cookie accessTokenCookie = new Cookie(jwtCookieName, tokenResponse.getAccessToken());
        setCookieProperties(accessTokenCookie, (int) (tokenResponse.getExpiresIn() / 1000));
        response.addCookie(accessTokenCookie);

        // Set refresh token cookie
        Cookie refreshTokenCookie = new Cookie(refreshCookieName, tokenResponse.getRefreshToken());
        setCookieProperties(refreshTokenCookie, (int) (15 * 60)); // 15 minutes
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping(path = "/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@CookieValue(name = "${jwt.refresh.cookie.name}") String refreshToken, HttpServletResponse response) {
        TokenResponse tokenResponse = userService.refreshToken(refreshToken);

        // Update access token cookie
        Cookie accessTokenCookie = new Cookie(jwtCookieName, tokenResponse.getAccessToken());
        setCookieProperties(accessTokenCookie, (int) (tokenResponse.getExpiresIn() / 1000));
        response.addCookie(accessTokenCookie);

        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping(path = "/logout")
    public ResponseEntity<String> logout(@CookieValue(name = "${jwt.refresh.cookie.name}") String refreshToken, HttpServletResponse response) {
        userService.logout(refreshToken);

        // Clear cookies
        Cookie accessTokenCookie = new Cookie(jwtCookieName, "");
        accessTokenCookie.setMaxAge(0);
        setCookieProperties(accessTokenCookie, 0);
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie(refreshCookieName, "");
        refreshTokenCookie.setMaxAge(0);
        setCookieProperties(refreshTokenCookie, 0);
        response.addCookie(refreshTokenCookie);

        return ResponseEntity.ok("Logged out successfully");
    }

    private void setCookieProperties(Cookie cookie, int maxAge) {
        cookie.setHttpOnly(cookieHttpOnly);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        // For SameSite attribute, you might need to use additional configuration
        // as it's not directly supported in Spring Boot Cookie class
    }
}