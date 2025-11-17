package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.model.CustomUserDetails;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService  jwtService ;
    private final RefreshTokenService refreshTokenService;
    private final Environment environment;

    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(12);

    @Autowired
    public UserService(UserRepository userRepository, AuthenticationManager authenticationManager, JwtService jwtService,
                       RefreshTokenService refreshTokenService, Environment environment) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.environment = environment;
    }

    public User signup(User user) {
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        return userRepository.signup(user);
    }

    public AuthResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        if (authentication.isAuthenticated()) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User domainUser = userDetails.getDomainUser();
            String accessToken = jwtService.generateAccessToken(domainUser);
            String refreshToken = jwtService.generateRefreshToken(domainUser);
            refreshTokenService.createRefreshToken(domainUser, refreshToken, jwtService.extractExpiration(refreshToken).toInstant());

            ResponseCookie accessCookie = jwtService.buildAccessTokenCookie(accessToken);
            ResponseCookie refreshCookie = jwtService.buildRefreshTokenCookie(refreshToken);
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            boolean isDev = java.util.Arrays.asList(environment.getActiveProfiles()).stream()
                    .anyMatch(p -> "dev".equalsIgnoreCase(p));

            return AuthResponse.builder()
                    .message("Login successful")
                    .username(domainUser.getUsername())
                    .accessToken(isDev ? accessToken : null)
                    .refreshToken(isDev ? refreshToken : null)
                    .accessTokenExpiresAt(jwtService.extractExpiration(accessToken).toInstant())
                    .refreshTokenExpiresAt(jwtService.extractExpiration(refreshToken).toInstant())
                    .build();
        }
        throw new RuntimeException("Authentication failed");
    }

    public User getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user");
        }
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = principal.getDomainUser();
        user.setPassword(null);
        return user;
    }
}
