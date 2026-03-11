package com.quickcash.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AdminApiKeyFilter adminApiKeyFilter;
    private final B2bApiKeyFilter b2bApiKeyFilter;
    private final IdempotencyFilter idempotencyFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AdminApiKeyFilter adminApiKeyFilter,
                          B2bApiKeyFilter b2bApiKeyFilter, IdempotencyFilter idempotencyFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.adminApiKeyFilter = adminApiKeyFilter;
        this.b2bApiKeyFilter = b2bApiKeyFilter;
        this.idempotencyFilter = idempotencyFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/health", "/h2-console/**", "/actuator/**").permitAll()
                        .requestMatchers("/users/createUser", "/users/updateUserLocation", "/users/updateToken").permitAll()
                        .requestMatchers("/cash/requestCash").permitAll()
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/register-pin", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/selcom/callback").permitAll()
                        .requestMatchers("/api/v1/bolt/webhook").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/b2b/**").hasRole("B2B")
                        .anyRequest().authenticated()
                )
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .addFilterBefore(b2bApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(adminApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(idempotencyFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
