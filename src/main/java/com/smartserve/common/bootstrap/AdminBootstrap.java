package com.smartserve.common.bootstrap;

import com.smartserve.user.entity.UserEntity;
import com.smartserve.user.enums.Role;
import com.smartserve.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.password}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.full-name}")
    private String adminFullName;

    @Override
    public void run(String... args) throws Exception {
        String username = adminUsername.trim();

        if(userRepository.existsByUsername(username)) {
            return;
        }

        UserEntity admin =  new UserEntity();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFullName(adminFullName);
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        userRepository.save(admin);
    }
}
