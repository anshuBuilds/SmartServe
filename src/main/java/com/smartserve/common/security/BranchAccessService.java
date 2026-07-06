package com.smartserve.common.security;

import com.smartserve.common.exception.ForbiddenException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.restaurant.repository.BranchRepository;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BranchAccessService {
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public Long resolveKitchenBranch(Authentication authentication, Long requestedBranchId) {
        UserEntity user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated user no longer exists"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ForbiddenException("User account is inactive");
        }
        if (user.getRole() == Role.KITCHEN) {
            if (user.getBranch() == null) {
                throw new ForbiddenException("Kitchen user is not assigned to a branch");
            }
            return user.getBranch().getId();
        }
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            throw new ForbiddenException("User cannot access the kitchen module");
        }
        if (requestedBranchId == null) {
            throw new ForbiddenException("branchId is required for managers and administrators");
        }
        if (!branchRepository.existsById(requestedBranchId)) {
            throw new ResourceNotFoundException("Branch not found");
        }
        return requestedBranchId;
    }
}
