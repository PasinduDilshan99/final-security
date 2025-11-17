package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private String message;
    private String username;
    private Instant accessTokenExpiresAt;
    private Instant refreshTokenExpiresAt;
}
