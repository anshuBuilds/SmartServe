package com.smartserve.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartserve.common.exception.ForbiddenException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BranchAccessServiceTest {

    @Mock UserRepository userRepository;
    @Mock BranchRepository branchRepository;
    @Mock Authentication authentication;

    private BranchAccessService service;

    @BeforeEach
    void setUp() {
        service = new BranchAccessService(userRepository, branchRepository);
        when(authentication.getName()).thenReturn("chef");
    }

    @Test
    void kitchenUserIsAlwaysScopedToAssignedBranch() {
        when(userRepository.findByUsername("chef"))
                .thenReturn(Optional.of(user(Role.KITCHEN, true, branch(4L))));

        assertEquals(4L, service.resolveKitchenBranch(authentication, 99L));
        verify(branchRepository, never()).existsById(99L);
    }

    @Test
    void kitchenUserWithoutBranchIsRejected() {
        when(userRepository.findByUsername("chef"))
                .thenReturn(Optional.of(user(Role.KITCHEN, true, null)));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.resolveKitchenBranch(authentication, null)
        );

        assertEquals("Kitchen user is not assigned to a branch", exception.getMessage());
    }

    @Test
    void managerCanSelectExistingBranch() {
        when(userRepository.findByUsername("chef"))
                .thenReturn(Optional.of(user(Role.MANAGER, true, null)));
        when(branchRepository.existsById(9L)).thenReturn(true);

        assertEquals(9L, service.resolveKitchenBranch(authentication, 9L));
    }

    @Test
    void administratorCanSelectExistingBranch() {
        when(userRepository.findByUsername("chef"))
                .thenReturn(Optional.of(user(Role.ADMIN, true, null)));
        when(branchRepository.existsById(9L)).thenReturn(true);

        assertEquals(9L, service.resolveKitchenBranch(authentication, 9L));
    }

    @Test
    void managerMustProvideBranchId() {
        when(userRepository.findByUsername("chef"))
                .thenReturn(Optional.of(user(Role.MANAGER, true, null)));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.resolveKitchenBranch(authentication, null)
        );

        assertEquals("branchId is required for managers and administrators", exception.getMessage());
    }

    @Test
    void missingRequestedBranchReturnsNotFound() {
        when(userRepository.findByUsername("chef"))
                .thenReturn(Optional.of(user(Role.ADMIN, true, null)));
        when(branchRepository.existsById(9L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.resolveKitchenBranch(authentication, 9L)
        );

        assertEquals("Branch not found", exception.getMessage());
    }

    @Test
    void waiterCannotAccessKitchenModule() {
        when(userRepository.findByUsername("chef"))
                .thenReturn(Optional.of(user(Role.WAITER, true, branch(4L))));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.resolveKitchenBranch(authentication, 4L)
        );

        assertEquals("User cannot access the kitchen module", exception.getMessage());
    }

    @Test
    void inactiveUserIsRejectedBeforeRoleChecks() {
        when(userRepository.findByUsername("chef"))
                .thenReturn(Optional.of(user(Role.ADMIN, false, null)));

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.resolveKitchenBranch(authentication, 4L)
        );

        assertEquals("User account is inactive", exception.getMessage());
    }

    @Test
    void deletedAuthenticatedUserIsRejected() {
        when(userRepository.findByUsername("chef")).thenReturn(Optional.empty());

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> service.resolveKitchenBranch(authentication, 4L)
        );

        assertEquals("Authenticated user no longer exists", exception.getMessage());
    }

    private UserEntity user(Role role, boolean active, Branch branch) {
        UserEntity user = new UserEntity();
        user.setRole(role);
        user.setActive(active);
        user.setBranch(branch);
        return user;
    }

    private Branch branch(Long id) {
        Branch branch = new Branch();
        ReflectionTestUtils.setField(branch, "id", id);
        return branch;
    }
}
