package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    private Integer id;
    private Integer userId;
    private String token;
    private LocalDateTime expiryDate;
    private boolean revoked = false;
}