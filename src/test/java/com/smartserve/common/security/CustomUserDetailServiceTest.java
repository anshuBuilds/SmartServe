package com.smartserve.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks CustomUserDetailService service;

    @Test
    void wrapsDatabaseUserWithRoleAndActiveState() {
        UserEntity user = new UserEntity();
        user.setUsername("chef");
        user.setPassword("encoded");
        user.setRole(Role.KITCHEN);
        user.setActive(true);
        when(userRepository.findByUsername("chef")).thenReturn(Optional.of(user));

        var details = service.loadUserByUsername("chef");

        assertEquals("chef", details.getUsername());
        assertEquals("encoded", details.getPassword());
        assertTrue(details.isEnabled());
        assertEquals("ROLE_KITCHEN", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void inactiveDatabaseUserIsDisabled() {
        UserEntity user = new UserEntity();
        user.setUsername("chef");
        user.setPassword("encoded");
        user.setRole(Role.KITCHEN);
        user.setActive(false);
        when(userRepository.findByUsername("chef")).thenReturn(Optional.of(user));

        assertFalse(service.loadUserByUsername("chef").isEnabled());
    }

    @Test
    void unknownUsernameUsesSpringSecurityException() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing")
        );

        assertEquals("missing", exception.getMessage());
    }
}
