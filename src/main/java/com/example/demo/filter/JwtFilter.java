package com.example.demo.filter;

import com.example.demo.model.CustomUserDetails;
import com.example.demo.model.RefreshToken;
import com.example.demo.service.CustomUserDetailsService;
import com.example.demo.service.JwtService;
import com.example.demo.service.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final RefreshTokenService refreshTokenService;

    public JwtFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService, RefreshTokenService refreshTokenService){
        this.jwtService=jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Optional<String> accessTokenOpt = jwtService.resolveAccessToken(request);
        String username = null;
        boolean accessTokenExpired = false;

        if (accessTokenOpt.isPresent()) {
            try {
                username = jwtService.extractUsername(accessTokenOpt.get());
            } catch (io.jsonwebtoken.ExpiredJwtException ex) {
                accessTokenExpired = true;
                username = ex.getClaims().getSubject();
            } catch (Exception ex) {
                username = null;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            if (!accessTokenExpired && jwtService.validateToken(accessTokenOpt.get(), userDetails)) {
                setAuthentication(userDetails, request);
            } else if (accessTokenExpired) {
                handleRefreshFlow(request, response, (CustomUserDetails) userDetails);
            }
        }

        filterChain.doFilter(request,response);

    }

    private void handleRefreshFlow(HttpServletRequest request, HttpServletResponse response, CustomUserDetails userDetails) {
        Optional<String> refreshTokenOpt = jwtService.resolveRefreshToken(request);
        if (refreshTokenOpt.isEmpty()) {
            return;
        }
        String refreshToken = refreshTokenOpt.get();
        Optional<RefreshToken> tokenFromDb = refreshTokenService.validateRefreshToken(refreshToken)
                .filter(rt -> rt.getUserId().equals(userDetails.getDomainUser().getId()));
        if (tokenFromDb.isEmpty() || !jwtService.canRefresh(refreshToken, userDetails)) {
            return;
        }
        String newAccessToken = jwtService.generateAccessToken(userDetails.getDomainUser());
        response.addHeader(HttpHeaders.SET_COOKIE, jwtService.buildAccessTokenCookie(newAccessToken).toString());
        setAuthentication(userDetails, request);
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
