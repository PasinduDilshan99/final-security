package com.example.demo.repository;

import com.example.demo.model.RefreshToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class RefreshTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public RefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RefreshToken save(RefreshToken refreshToken) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO refresh_tokens (user_id, token, expiry_date, revoked) VALUES (?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, refreshToken.getUserId());
            ps.setString(2, refreshToken.getToken());
            ps.setTimestamp(3, Timestamp.from(refreshToken.getExpiryDate()));
            ps.setBoolean(4, refreshToken.isRevoked());
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            refreshToken.setId(keyHolder.getKey().longValue());
        }
        return refreshToken;
    }

    public Optional<RefreshToken> findByToken(String token) {
        String sql = "SELECT id, user_id, token, expiry_date, revoked FROM refresh_tokens WHERE token = ?";
        List<RefreshToken> result = jdbcTemplate.query(sql, (rs, rowNum) -> RefreshToken.builder()
                .id(rs.getLong("id"))
                .userId(rs.getInt("user_id"))
                .token(rs.getString("token"))
                .expiryDate(rs.getTimestamp("expiry_date").toInstant())
                .revoked(rs.getBoolean("revoked"))
                .build(), token);
        return result.stream().findFirst();
    }

    public void revokeAllForUser(Integer userId) {
        jdbcTemplate.update("UPDATE refresh_tokens SET revoked = true WHERE user_id = ?", userId);
    }

    public void revokeToken(String token) {
        jdbcTemplate.update("UPDATE refresh_tokens SET revoked = true WHERE token = ?", token);
    }

    public void deleteExpired(Instant now) {
        jdbcTemplate.update("DELETE FROM refresh_tokens WHERE expiry_date < ?", Timestamp.from(now));
    }
}
