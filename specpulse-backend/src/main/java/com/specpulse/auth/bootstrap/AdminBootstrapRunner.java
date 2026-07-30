package com.specpulse.auth.bootstrap;

import com.specpulse.auth.entity.RoleEntity;
import com.specpulse.auth.entity.UserEntity;
import com.specpulse.auth.repository.RoleRepository;
import com.specpulse.auth.repository.PermissionRepository;
import com.specpulse.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import com.specpulse.auth.entity.PermissionEntity;
import com.specpulse.auth.security.RbacPermissions;
import com.specpulse.auth.security.RbacRoles;

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
    private final PermissionRepository permissionRepository;
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
            PermissionRepository permissionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Ensure base RBAC roles and permissions exist (idempotent).
        PermissionEntity docsAccess = ensurePermission(RbacPermissions.API_DOCS_ACCESS);
        PermissionEntity testExecute = ensurePermission(RbacPermissions.API_TEST_EXECUTE);

        RoleEntity adminRole = ensureRole(RbacRoles.ADMIN, Set.of(docsAccess, testExecute));
        RoleEntity userRole = ensureRole(RbacRoles.USER, Set.of(docsAccess, testExecute));
        ensureRole(RbacRoles.VIEWER, Set.of(docsAccess));

        // Only bootstrap the first admin user when the database is truly empty.
        if (userRepository.count() > 0) {
            return;
        }

        UserEntity adminUser = new UserEntity();
        adminUser.setUsername(adminUsername);
        adminUser.setEmail(adminEmail);
        adminUser.setPasswordHash(passwordEncoder.encode(adminPassword));
        adminUser.setEnabled(true);
        adminUser.setRoles(Set.of(adminRole));

        userRepository.save(adminUser);
    }

    private PermissionEntity ensurePermission(String permissionName) {
        return permissionRepository.findByNameAndDeletedAtIsNull(permissionName)
                .map(p -> {
                    p.setEnabled(true);
                    return permissionRepository.save(p);
                })
                .orElseGet(() -> {
                    PermissionEntity p = new PermissionEntity();
                    p.setName(permissionName);
                    p.setEnabled(true);
                    p.setDescription(null);
                    return permissionRepository.save(p);
                });
    }

    private RoleEntity ensureRole(String roleName, Set<PermissionEntity> permissions) {
        RoleEntity role = roleRepository.findByNameAndDeletedAtIsNull(roleName)
                .map(r -> {
                    r.setEnabled(true);
                    return r;
                })
                .orElseGet(() -> {
                    RoleEntity r = new RoleEntity();
                    r.setName(roleName);
                    r.setEnabled(true);
                    return r;
                });

        role.setPermissions(permissions);
        return roleRepository.save(role);
    }
}
