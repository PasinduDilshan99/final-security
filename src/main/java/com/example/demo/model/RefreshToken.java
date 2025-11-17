package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken {
    private Integer id;
    private Integer userId;
    private String token;
    private LocalDateTime expiryDate;
    private boolean revoked;
    private LocalDateTime createdAt;
}