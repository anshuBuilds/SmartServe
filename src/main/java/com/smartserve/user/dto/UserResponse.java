package com.smartserve.user.dto;

import com.smartserve.user.enums.Role;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private Role role;
    private Boolean active;
    private Long branchId;
    private String branchName;
    private Instant createdAt;
    private Instant updatedAt;
}