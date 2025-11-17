package com.example.demo.service;

import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.refresh.expiration.time}")
    private long refreshTokenDurationMs;

    public Optional<RefreshToken> createRefreshToken(Integer userId) {
        User user = userRepository.getUserByUsername(
                userRepository.getUserByUsername(userId.toString()) // This needs fixing
        ).orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setRevoked(false);

        userRepository.saveRefreshToken(refreshToken);
        return Optional.of(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return userRepository.findRefreshToken(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            userRepository.revokeRefreshToken(token.getToken());
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    public void deleteByUserId(Integer userId) {
        userRepository.revokeAllUserRefreshTokens(userId);
    }
}