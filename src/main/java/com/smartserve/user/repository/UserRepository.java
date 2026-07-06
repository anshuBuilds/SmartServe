package com.smartserve.user.repository;

import com.smartserve.user.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity,Long> {

    @EntityGraph(attributePaths = "branch")
    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}

