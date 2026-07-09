package com.smartserve.user.Controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.user.dto.CreateUserRequest;
import com.smartserve.user.dto.UpdateUserBranchRequest;
import com.smartserve.user.dto.UpdateUserStatusRequest;
import com.smartserve.user.dto.UserResponse;
import com.smartserve.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
        return ApiResponse.success(userService.getUserByUsername(authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created", userService.createUser(request)));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success(userService.getUser(userId));
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success("User status updated",
                userService.updateUserStatus(userId, request, authentication.getName()));
    }

    @PatchMapping("/{userId}/branch")
    public ApiResponse<UserResponse> updateUserBranch(
            @PathVariable Long userId,
            @RequestBody UpdateUserBranchRequest request
    ) {
        return ApiResponse.success("User branch updated", userService.updateUserBranch(userId, request));
    }
}
