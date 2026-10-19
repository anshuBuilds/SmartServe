package com.smartserve.common.security;

import com.smartserve.common.exception.BadRequestException;
import com.smartserve.common.exception.ForbiddenException;
import com.smartserve.common.exception.ResourceNotFoundException;
import com.smartserve.restaurant.repository.RestaurantRepository;
import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides which restaurant's menu a staff request works on. Administrators choose a
 * restaurant explicitly; everyone else is limited to the restaurant of their branch.
 * Which roles may read or write is still decided by SecurityConfig.
 */
@Service
@RequiredArgsConstructor
public class MenuAccessService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional(readOnly = true)
    public Long resolveRestaurant(Authentication authentication, Long requestedRestaurantId) {
        if (isAdmin(authentication)) {
            if (requestedRestaurantId == null) {
                throw new BadRequestException("restaurantId is required for administrators");
            }
            if (!restaurantRepository.existsById(requestedRestaurantId)) {
                throw new ResourceNotFoundException("Restaurant not found");
            }
            return requestedRestaurantId;
        }

        UserEntity user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated user no longer exists"));
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ForbiddenException("User account is inactive");
        }
        if (user.getBranch() == null) {
            throw new ForbiddenException("User is not assigned to a branch");
        }

        Long restaurantId = user.getBranch().getRestaurant().getId();
        if (requestedRestaurantId != null && !requestedRestaurantId.equals(restaurantId)) {
            throw new ForbiddenException("You can only access your own restaurant's menu");
        }
        return restaurantId;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
