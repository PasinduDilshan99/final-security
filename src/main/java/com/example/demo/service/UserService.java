package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.model.CustomUserDetails;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService  jwtService ;
    private final RefreshTokenService refreshTokenService;

    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(12);

    @Autowired
    public UserService(UserRepository userRepository, AuthenticationManager authenticationManager, JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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

            return AuthResponse.builder()
                    .message("Login successful")
                    .username(domainUser.getUsername())
                    .roles(domainUser.getRoles())
                    .privileges(domainUser.getPrivileges())
                    .accessTokenExpiresAt(jwtService.extractExpiration(accessToken).toInstant())
                    .refreshTokenExpiresAt(jwtService.extractExpiration(refreshToken).toInstant())
                    .build();
        }
        throw new RuntimeException("Authentication failed");
    }
}
