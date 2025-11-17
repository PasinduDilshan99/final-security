package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> getUserByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";

        try {
            User user = jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) -> User.builder()
                    .id(rs.getInt("user_id"))
                    .username(rs.getString("username"))
                    .password(rs.getString("password"))
                    .firstName(rs.getString("first_name"))
                    .middleName(rs.getString("middle_name"))
                    .lastName(rs.getString("last_name"))
                    .email(rs.getString("email"))
                    .mobileNumber1(rs.getString("mobile_number1"))
                    .mobileNumber2(rs.getString("mobile_number2"))
                    .build());

            if (user != null) {
                user.setRoles(fetchRoles(user.getId()));
                user.setPrivileges(fetchPrivileges(user.getId()));
            }
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    public User signup(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO user (username, password, first_name, middle_name, last_name, email, mobile_number1, mobile_number2) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFirstName());
            ps.setString(4, user.getMiddleName());
            ps.setString(5, user.getLastName());
            ps.setString(6, user.getEmail());
            ps.setString(7, user.getMobileNumber1());
            ps.setString(8, user.getMobileNumber2());
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            user.setId(keyHolder.getKey().intValue());
        }
        Integer roleId = getRoleIdByName("ROLE_USER");
        if (roleId != null) {
            jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)",
                    user.getId(), roleId);
            user.setRoles(fetchRoles(user.getId()));
            user.setPrivileges(fetchPrivileges(user.getId()));
        }
        return user;
    }

    private Integer getRoleIdByName(String roleName) {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM roles WHERE name = ?", Integer.class, roleName);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private Set<String> fetchRoles(Integer userId) {
        String sql = """
                SELECT r.name FROM roles r 
                INNER JOIN user_roles ur ON r.id = ur.role_id 
                WHERE ur.user_id = ?
                """;
        return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"), userId));
    }

    private Set<String> fetchPrivileges(Integer userId) {
        String sql = """
                SELECT DISTINCT p.name FROM privileges p 
                INNER JOIN role_privileges rp ON p.id = rp.privilege_id
                INNER JOIN user_roles ur ON rp.role_id = ur.role_id
                WHERE ur.user_id = ?
                """;
        return new HashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("name"), userId));
    }
}
