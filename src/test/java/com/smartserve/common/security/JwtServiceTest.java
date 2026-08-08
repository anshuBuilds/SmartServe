package com.smartserve.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "a-very-long-test-secret-key-with-at-least-32-bytes";

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService();
        ReflectionTestUtils.setField(service, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(service, "jwtExpirationMs", 60_000L);
    }

    @Test
    void generatedTokenContainsUsernameAndValidatesForSameUser() {
        String token = service.generateToken(user("anshu"));
        var userDetails = User.withUsername("anshu").password("ignored").roles("ADMIN").build();

        assertEquals("anshu", service.extractUsername(token));
        assertTrue(service.isTokenValid(token, userDetails));
    }

    @Test
    void tokenIsInvalidForDifferentUsername() {
        String token = service.generateToken(user("anshu"));
        var differentUser = User.withUsername("someone-else").password("ignored").roles("ADMIN").build();

        assertFalse(service.isTokenValid(token, differentUser));
    }

    @Test
    void expiredTokenIsNotValid() {
        ReflectionTestUtils.setField(service, "jwtExpirationMs", -1_000L);
        String token = service.generateToken(user("anshu"));
        var userDetails = User.withUsername("anshu").password("ignored").roles("ADMIN").build();

        assertFalse(service.isTokenValid(token, userDetails));
    }

    @Test
    void exposesConfiguredExpirationForLoginResponse() {
        assertEquals(60_000L, service.getExpirationMs());
    }

    private UserEntity user(String username) {
        UserEntity user = new UserEntity();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setUsername(username);
        user.setRole(Role.ADMIN);
        return user;
    }
}
