package com.quickcash.auth;

import com.quickcash.domain.IdempotencyRecord;
import com.quickcash.service.IdempotencyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * For POST /api/v1/cash/request and POST /api/v1/cash/send: when X-Idempotency-Key is present,
 * return cached response if key was already used; otherwise run and cache response.
 */
@Component
@Order(100)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.idempotency");
    private final IdempotencyService idempotencyService;

    public IdempotencyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isCashRequest = "POST".equalsIgnoreCase(request.getMethod()) && path != null
                && (path.endsWith("/api/v1/cash/request") || path.endsWith("/api/v1/cash/send"));
        String key = request.getHeader("X-Idempotency-Key");
        if (!isCashRequest || key == null || key.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        if (!(principal instanceof UUID userId)) {
            filterChain.doFilter(request, response);
            return;
        }
        String userIdStr = userId.toString();
        var cached = idempotencyService.findValid(key, userIdStr);
        if (cached.isPresent()) {
            IdempotencyRecord r = cached.get();
            log.info("Idempotency cache hit: key={}, userId={}, status={}", key, userIdStr, r.getResponseStatus());
            response.setStatus(r.getResponseStatus());
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            if (r.getResponseBody() != null && !r.getResponseBody().isEmpty()) {
                response.getWriter().write(r.getResponseBody());
            }
            return;
        }
        log.debug("Idempotency miss: key={}, userId={}, proceeding", key, userIdStr);
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);
        byte[] body = wrapper.getContentAsByteArray();
        int status = wrapper.getStatus();
        idempotencyService.save(key, userIdStr, status, body.length > 0 ? new String(body, StandardCharsets.UTF_8) : null);
        wrapper.copyBodyToResponse();
    }
}
