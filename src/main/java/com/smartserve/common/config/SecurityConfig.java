package com.smartserve.common.config;

import com.smartserve.common.security.JwtAuthenticationFilter;
import com.smartserve.common.security.GuestRateLimitFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final GuestRateLimitFilter guestRateLimitFilter;
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/guest/**", "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/kitchen/**").hasAnyRole("ADMIN", "MANAGER", "KITCHEN")

                        .requestMatchers(HttpMethod.GET, "/api/menu/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER", "KITCHEN")
                        .requestMatchers(HttpMethod.POST, "/api/menu/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/menu/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/menu/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/orders").hasAnyRole("ADMIN", "MANAGER", "WAITER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER", "KITCHEN")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/serve")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/cancel")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status")
                        .hasAnyRole("ADMIN", "MANAGER")

                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/restaurants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/restaurants/*/branches").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/branches/*")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER", "KITCHEN")
                        .requestMatchers(HttpMethod.POST, "/api/branches/*/tables").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/branches/*/tables")
                        .hasAnyRole("ADMIN", "MANAGER", "WAITER", "KITCHEN")
                        .requestMatchers(HttpMethod.PATCH, "/api/branches/*/tables/*/status")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/branches/*/tables/*/qr-token/rotate")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/webhooks/razorpay"
                        ).permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(guestRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
