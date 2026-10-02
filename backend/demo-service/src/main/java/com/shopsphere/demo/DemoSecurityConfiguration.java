package com.shopsphere.demo;

import com.shopsphere.auth.security.JwtAuthenticationFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Replaces auth-service's chain, which secures every path except the auth endpoints. In the demo
 * the catalog must stay publicly readable and the admin surface must be role checked, which is the
 * gateway's job in the real topology.
 *
 * <p>Order of filters matters: {@link DemoIdentityFilter} is stateless, enforces the gateway rules
 * and publishes the authenticated principal, then the service's own filter resolves that principal
 * to a user record for {@code /api/auth/me}.
 */
@Configuration
@EnableWebSecurity
public class DemoSecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain demoSecurityFilterChain(HttpSecurity http,
                                                DemoIdentityFilter identityFilter,
                                                JwtAuthenticationFilter jwtAuthenticationFilter,
                                                CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        .requestMatchers("/actuator/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(identityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, DemoIdentityFilter.class)
                .build();
    }

    /**
     * The deployed frontend gets a generated domain, so allowed origins must come from the
     * environment (Render sets {@code CORS_ALLOWED_ORIGINS}). The nested property default keeps it
     * overridable with {@code -Ddemo.cors.allowed-origins=...} for local runs and tests.
     *
     * <p>Leaving it unset means no allowed origins at all, which is the right default: the browser
     * then refuses any cross-origin call, so a forgotten variable fails loudly instead of opening the
     * API to every site.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${demo.cors.allowed-origins:${CORS_ALLOWED_ORIGINS:}}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            List<String> origins = Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .toList();
            configuration.setAllowedOrigins(origins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
