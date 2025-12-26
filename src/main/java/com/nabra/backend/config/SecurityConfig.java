package com.nabra.backend.config;

import com.nabra.backend.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.nabra.backend.security.principal.DbUserDetailsService;

/**
 * Security configuration for the application.
 * Configures JWT-based stateless authentication, CSRF protection,
 * and authorization rules for API endpoints.
 */
@Configuration
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true,
    prePostEnabled = true
)
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final DbUserDetailsService userDetailsService;

  /**
   * Configure password encoder (BCrypt).
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // Cost 12 for stronger hashing
  }

  /**
   * Configure authentication manager with DAO provider.
   */
  @Bean
  public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  /**
   * Configure security filter chain.
   * - Disables CSRF (stateless JWT doesn't need it)
   * - Uses STATELESS session policy (no sessions)
   * - Permits public endpoints (auth, swagger, health)
   * - Requires authentication for all other endpoints
   * - Adds JWT filter before username/password filter
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // CSRF disabled for stateless JWT-based API
        .csrf(csrf -> csrf.disable())

        // Stateless session policy - no session management
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Authorization rules
        .authorizeHttpRequests(auth -> auth
            // Public endpoints - no authentication required
            .requestMatchers(
                "/v3/api-docs/**",           // OpenAPI docs
                "/swagger-ui/**",             // Swagger UI
                "/swagger-ui.html",           // Swagger UI HTML
                "/api/v1/auth/register",      // User registration
                "/api/v1/auth/login",         // User login
                "/api/v1/auth/validate",      // Token validation
                "/actuator/health",           // Health check
                "/actuator/health/**"         // Health check endpoints
            ).permitAll()

            // All other requests require authentication
            .anyRequest().authenticated()
        )

        // Add JWT authentication filter before standard filter
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
