package com.smartserve.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GuestRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Window> clients = new ConcurrentHashMap<>();
    @Value("${app.guest-rate-limit.requests-per-minute:60}") private int limit;

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/guest/");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain chain) throws ServletException, IOException {
        long minute = Instant.now().getEpochSecond() / 60;
        String key = request.getRemoteAddr();
        Window window = clients.compute(key, (k, old) -> old == null || old.minute != minute ? new Window(minute) : old);
        if (window.count.incrementAndGet() > limit) {
            response.setStatus(429); response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many guest requests. Please try again shortly.\"}");
            return;
        }
        if (clients.size() > 10000) clients.entrySet().removeIf(e -> e.getValue().minute < minute - 2);
        chain.doFilter(request, response);
    }
    private static final class Window { final long minute; final AtomicInteger count = new AtomicInteger(); Window(long minute){this.minute=minute;} }
}
