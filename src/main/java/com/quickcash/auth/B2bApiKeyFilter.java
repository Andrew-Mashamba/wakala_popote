package com.quickcash.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class B2bApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.b2b.api-key:}")
    private String b2bApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/v1/b2b/")) {
            String key = request.getHeader("X-B2B-API-Key");
            if (StringUtils.hasText(b2bApiKey) && b2bApiKey.equals(key)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        "b2b", null, List.of(new SimpleGrantedAuthority("ROLE_B2B")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        filterChain.doFilter(request, response);
    }
}
