package com.specpulse.auth.bootstrap;

import com.specpulse.auth.entity.RoleEntity;
import com.specpulse.auth.entity.UserEntity;
import com.specpulse.auth.repository.RoleRepository;
import com.specpulse.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Creates the first ADMIN user in an empty database.
 *
 * Defaults (can be overridden via env vars):
 * - username: specpulse
 * - password: specpulse
 * - email: specpulse@example.com
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${SPECPULSE_ADMIN_USERNAME:specpulse}")
    private String adminUsername;

    @Value("${SPECPULSE_ADMIN_PASSWORD:specpulse}")
    private String adminPassword;

    @Value("${SPECPULSE_ADMIN_EMAIL:specpulse@example.com}")
    private String adminEmail;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Only bootstrap when the database is truly empty.
        if (userRepository.count() > 0) {
            return;
        }

        RoleEntity adminRole = roleRepository.findByNameAndEnabledTrueAndDeletedAtIsNull("ADMIN")
                .orElseGet(() -> {
                    RoleEntity role = new RoleEntity();
                    role.setName("ADMIN");
                    role.setEnabled(true);
                    return roleRepository.save(role);
                });

        UserEntity adminUser = new UserEntity();
        adminUser.setUsername(adminUsername);
        adminUser.setEmail(adminEmail);
        adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
        adminUser.setEnabled(true);
        adminUser.setRoles(Set.of(adminRole));

        userRepository.save(adminUser);
    }
}
