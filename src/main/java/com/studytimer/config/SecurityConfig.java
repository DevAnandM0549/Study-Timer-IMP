package com.studytimer.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus; // Required for HttpStatusEntryPoint
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.HttpStatusEntryPoint; // Required for HttpStatusEntryPoint
// Removed unused Customizer import

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions().disable())

                // Disable HTTP Basic to prevent browser popup
                .httpBasic(basic -> basic.disable())

                // Handle unauthorized requests by returning 401 without WWW-Authenticate header
                .exceptionHandling(handling -> handling.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                ))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/h2-console/**", "/static/**", "/", "/*.html", "/*.css", "/*.js").permitAll()
                        .requestMatchers("/api/**").permitAll()  // Allow all, handle auth in controllers
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );


        return http.build();
    }
}