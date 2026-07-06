package com.smartserve.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.user.dto.CreateUserRequest;
import com.smartserve.user.dto.UpdateUserBranchRequest;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import com.smartserve.user.service.UserService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock BranchRepository branchRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, branchRepository);
    }

    @Test
    void kitchenUserRequiresBranch() {
        CreateUserRequest request = request(Role.KITCHEN, null);
        assertThrows(BadRequestException.class, () -> service.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createsKitchenUserWithActiveBranch() {
        Branch branch = new Branch();
        branch.setName("Main");
        branch.setActive(true);
        ReflectionTestUtils.setField(branch, "id", 4L);
        when(branchRepository.findById(4L)).thenReturn(Optional.of(branch));
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createUser(request(Role.KITCHEN, 4L));

        assertEquals(4L, response.getBranchId());
        assertEquals("Main", response.getBranchName());
    }

    @Test
    void cannotClearKitchenUsersBranch() {
        UserEntity user = new UserEntity();
        user.setRole(Role.KITCHEN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        UpdateUserBranchRequest request = new UpdateUserBranchRequest();

        assertThrows(BadRequestException.class, () -> service.updateUserBranch(2L, request));
    }

    private CreateUserRequest request(Role role, Long branchId) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("chef");
        request.setPassword("password1");
        request.setFullName("Chef One");
        request.setRole(role);
        request.setBranchId(branchId);
        return request;
    }
}
