package com.lastcup.api.security;

import com.lastcup.api.domain.auth.service.AccessTokenBlacklistService;
import com.lastcup.api.global.error.JwtErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final JwtProvider jwtProvider;
    private final AccessTokenBlacklistService accessTokenBlacklistService;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            AccessTokenBlacklistService accessTokenBlacklistService
    ) {
        this.jwtProvider = jwtProvider;
        this.accessTokenBlacklistService = accessTokenBlacklistService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (PATH_MATCHER.match("/webjars/**", uri)) {
            return true;
        }
        if (PATH_MATCHER.match("/favicon.ico", uri)) {
            return true;
        }
        if (PATH_MATCHER.match("/actuator/**", uri)) {
            return true;
        }
        if (PATH_MATCHER.match("/api/v1/auth/**", uri)) {
            return true;
        }
        if (PATH_MATCHER.match("/swagger-ui/**", uri)) {
            return true;
        }
        if (PATH_MATCHER.match("/v3/api-docs/**", uri)) {
            return true;
        }
        if (PATH_MATCHER.match("/api-docs/**", uri)) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            authenticate(token);
            filterChain.doFilter(request, response);
        } catch (JwtValidationException ex) {
            request.setAttribute("authErrorCode", ex.getErrorCode());
            throw new JwtAuthenticationException(ex.getErrorCode(), ex);
        }
    }

    private void authenticate(String token) {
        jwtProvider.validate(token, "ACCESS");
        if (accessTokenBlacklistService.isBlacklisted(token)) {
            throw new JwtValidationException(JwtErrorCode.JWT_ACCESS_INVALID);
        }
        AuthUser authUser = jwtProvider.parseAccessToken(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authUser, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || header.isBlank()) {
            return null;
        }
        if (!header.startsWith("Bearer ")) {
            request.setAttribute("authErrorCode", JwtErrorCode.JWT_TOKEN_MISSING);
            return null;
        }
        return header.substring(7);
    }
}
