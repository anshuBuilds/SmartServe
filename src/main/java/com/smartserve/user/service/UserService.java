package com.smartserve.user.service;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.restaurant.entity.Branch;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.user.dto.CreateUserRequest;
import com.smartserve.user.dto.UpdateUserBranchRequest;
import com.smartserve.user.dto.UpdateUserStatusRequest;
import com.smartserve.user.dto.UserResponse;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRepository branchRepository;

    public UserResponse createUser(CreateUserRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already exists");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setRole(request.getRole());
        user.setActive(true);
        user.setBranch(resolveBranchForRole(request.getRole(), request.getBranchId()));
        return toUserResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return toUserResponse(findUser(userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        return toUserResponse(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found")));
    }

    public UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request, String currentUsername) {
        UserEntity user = findUser(userId);
        if (user.getUsername().equals(currentUsername)) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        user.setActive(request.getActive());
        return toUserResponse(user);
    }

    public UserResponse updateUserBranch(Long userId, UpdateUserBranchRequest request) {
        UserEntity user = findUser(userId);
        user.setBranch(resolveBranchForRole(user.getRole(), request.getBranchId()));
        return toUserResponse(user);
    }

    private Branch resolveBranchForRole(Role role, Long branchId) {
        if (role == Role.KITCHEN && branchId == null) {
            throw new BadRequestException("Kitchen users must be assigned to a branch");
        }
        if (branchId == null) {
            return null;
        }
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new BadRequestException("Branch is not active");
        }
        return branch;
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getFullName(), user.getRole(), user.getActive(),
                user.getBranch() == null ? null : user.getBranch().getId(),
                user.getBranch() == null ? null : user.getBranch().getName(),
                user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
