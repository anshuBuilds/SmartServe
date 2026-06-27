package com.smartserve.user.Controller;

import com.smartserve.common.response.ApiResponse;
import com.smartserve.user.dto.CreateUserRequest;
import com.smartserve.user.dto.UpdateUserStatusRequest;
import com.smartserve.user.dto.UserResponse;
import com.smartserve.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
        UserResponse user = userService.getUserByUsername(authentication.getName());
        return ApiResponse.success(user);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse user = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created", user));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        UserResponse user = userService.getUser(userId);
        return ApiResponse.success(user);
    }

    @PatchMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        UserResponse user = userService.updateUserStatus(
                userId,
                request,
                authentication.getName()
        );

        return ApiResponse.success("User status updated", user);
    }
}
