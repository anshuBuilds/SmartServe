package com.smartserve.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import io.jsonwebtoken.JwtException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtService jwtService;
    @Mock CustomUserDetailService userDetailService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtService, userDetailService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestWithoutBearerTokenContinuesUnauthenticated() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/orders"),
                new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validBearerTokenPopulatesSecurityContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader("Authorization", "Bearer valid-token");
        CustomUserDetails details = userDetails("manager");
        when(jwtService.extractUsername("valid-token")).thenReturn("manager");
        when(userDetailService.loadUserByUsername("manager")).thenReturn(details);
        when(jwtService.isTokenValid("valid-token", details)).thenReturn(true);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("manager", authentication.getName());
        assertSame(details, authentication.getPrincipal());
        assertEquals(List.of("ROLE_MANAGER"), authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).toList());
    }

    @Test
    void malformedOrExpiredBearerTokenContinuesUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders");
        request.addHeader("Authorization", "Bearer expired-token");
        when(jwtService.extractUsername("expired-token")).thenThrow(new JwtException("expired"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailService, never()).loadUserByUsername(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void swaggerRequestBypassesJwtParsingEvenWithHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        request.setServletPath("/swagger-ui/index.html");
        request.addHeader("Authorization", "Bearer ignored");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(jwtService, never()).extractUsername(org.mockito.ArgumentMatchers.any());
    }

    private CustomUserDetails userDetails(String username) {
        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword("encoded");
        user.setRole(Role.MANAGER);
        user.setActive(true);
        return new CustomUserDetails(user);
    }
}
