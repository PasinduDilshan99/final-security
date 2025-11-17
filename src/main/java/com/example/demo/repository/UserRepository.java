package com.example.demo.repository;

import com.example.demo.model.RefreshToken;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> getUserByUsername(String username) {
        String sql = """
            SELECT u.id, u.username, u.password, u.enabled 
            FROM users u 
            WHERE u.username = ?
            """;

        try {
            User user = jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) -> {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setPassword(rs.getString("password"));
                u.setEnabled(rs.getBoolean("enabled"));
                return u;
            });

            if (user != null) {
                user.setRoles(getUserRoles(user.getId()));
            }
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Role> getUserRoles(Integer userId) {
        String sql = """
            SELECT r.id, r.name 
            FROM roles r 
            JOIN user_roles ur ON r.id = ur.role_id 
            WHERE ur.user_id = ?
            """;

        return jdbcTemplate.query(sql, new Object[]{userId}, (rs, rowNum) -> {
            Role role = new Role();
            role.setId(rs.getInt("id"));
            role.setName(rs.getString("name"));
            return role;
        });
    }

    public void saveUser(User user) {
        String sql = "INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setBoolean(3, user.isEnabled());
            return ps;
        }, keyHolder);

        Integer userId = keyHolder.getKey().intValue();
        user.setId(userId);

        // Save user roles
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                addRoleToUser(userId, role.getId());
            }
        }
    }

    public void addRoleToUser(Integer userId, Integer roleId) {
        String sql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, userId, roleId);
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    // Refresh Token methods
    public void saveRefreshToken(RefreshToken refreshToken) {
        String sql = "INSERT INTO refresh_tokens (user_id, token, expiry_date) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql,
                refreshToken.getUserId(),
                refreshToken.getToken(),
                Timestamp.valueOf(refreshToken.getExpiryDate()));
    }

    public Optional<RefreshToken> findRefreshToken(String token) {
        String sql = """
            SELECT id, user_id, token, expiry_date, revoked, created_at 
            FROM refresh_tokens 
            WHERE token = ? AND revoked = false
            """;

        try {
            RefreshToken refreshToken = jdbcTemplate.queryForObject(sql, new Object[]{token}, (rs, rowNum) -> {
                RefreshToken rt = new RefreshToken();
                rt.setId(rs.getInt("id"));
                rt.setUserId(rs.getInt("user_id"));
                rt.setToken(rs.getString("token"));
                rt.setExpiryDate(rs.getTimestamp("expiry_date").toLocalDateTime());
                rt.setRevoked(rs.getBoolean("revoked"));
                rt.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return rt;
            });
            return Optional.ofNullable(refreshToken);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void revokeRefreshToken(String token) {
        String sql = "UPDATE refresh_tokens SET revoked = true WHERE token = ?";
        jdbcTemplate.update(sql, token);
    }

    public void revokeAllUserRefreshTokens(Integer userId) {
        String sql = "UPDATE refresh_tokens SET revoked = true WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
}