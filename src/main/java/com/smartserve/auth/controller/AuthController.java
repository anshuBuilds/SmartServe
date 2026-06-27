package com.smartserve.auth.controller;


import com.smartserve.auth.dto.AuthResponse;
import com.smartserve.auth.dto.LoginRequest;
import com.smartserve.common.response.ApiResponse;
import com.smartserve.common.security.CustomUserDetails;
import com.smartserve.common.security.JwtService;
import com.smartserve.user.dto.UserResponse;
import com.smartserve.user.entity.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                       request.getUsername().trim(),
                        request.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        UserEntity user = userDetails.getUser();

        String token = jwtService.generateToken(user);

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );

        AuthResponse authResponse = new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                userResponse
        );

        return ApiResponse.success("Login successful", authResponse);
    }
}
