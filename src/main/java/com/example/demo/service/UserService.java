package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(12);

    @Autowired
    public UserService(UserRepository userRepository,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public void signup(User user) {
        if (userRepository.usernameExists(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        user.setEnabled(true);

        // Assign default role
        Role defaultRole = new Role(1, "ROLE_USER"); // ROLE_USER
        user.setRoles(Collections.singletonList(defaultRole));

        userRepository.saveUser(user);
    }

    public TokenResponse login(User user) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );

        if (authentication.isAuthenticated()) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = jwtService.generateAccessToken(user.getUsername());

            // Create or update refresh token
            refreshTokenService.deleteByUserId(userDetails.getUser().getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("Failed to create refresh token"));

            return new TokenResponse(accessToken, refreshToken.getToken(), "Bearer", jwtService.getRefreshExpiration());
        }
        throw new RuntimeException("Authentication failed");
    }

    public TokenResponse refreshToken(String refreshToken) {
        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUserId)
                .map(userId -> {
                    User user = userRepository.getUserByUsername("") // This needs the actual user
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    String accessToken = jwtService.generateAccessToken(user.getUsername());
                    return new TokenResponse(accessToken, refreshToken, "Bearer", jwtService.getRefreshExpiration());
                })
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
    }

    public void logout(String refreshToken) {
        refreshTokenService.findByToken(refreshToken)
                .ifPresent(token -> userRepository.revokeRefreshToken(token.getToken()));
    }
}