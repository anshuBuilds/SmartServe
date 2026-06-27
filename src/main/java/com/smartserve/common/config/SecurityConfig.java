package com.smartserve.common.config;

import com.smartserve.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/swagger-ui.html","/swagger-ui/**", "/v3/api-docs/**","/webjars/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/menu/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER", "KITCHEN")
                        .requestMatchers(HttpMethod.POST, "/api/menu/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/menu/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/menu/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/orders")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER", "KITCHEN")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER", "KITCHEN")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/cancel")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER")

                        .requestMatchers("/api/users/me")
                        .authenticated()
                        .requestMatchers("/api/users/**")
                        .hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}