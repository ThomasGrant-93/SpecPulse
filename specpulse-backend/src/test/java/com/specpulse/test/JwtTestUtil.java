package com.specpulse.test;

import com.specpulse.auth.entity.PermissionEntity;
import com.specpulse.auth.entity.RoleEntity;
import com.specpulse.auth.entity.UserEntity;
import com.specpulse.auth.repository.UserRepository;
import com.specpulse.auth.security.JwtService;
import com.specpulse.auth.security.RbacPermissions;
import com.specpulse.auth.security.RbacRoles;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.BDDMockito.given;

public final class JwtTestUtil {

    private JwtTestUtil() {
    }

    public static String adminAccessToken(JwtService jwtService, long userId) {
        return jwtService.issueAccessToken(
                        userId,
                        "admin",
                        "admin@example.com",
                        true,
                        List.of(RbacRoles.ADMIN),
                        Set.of(),
                        Map.of()
                )
                .accessToken();
    }

    public static void stubEnabledAdmin(UserRepository userRepository, long userId) {
        RoleEntity adminRole = new RoleEntity();
        adminRole.setName(RbacRoles.ADMIN);
        adminRole.setEnabled(true);

        adminRole.setPermissions(Set.of(
                permission(RbacPermissions.API_DOCS_ACCESS),
                permission(RbacPermissions.API_TEST_EXECUTE)
        ));

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setEnabled(true);
        user.setRoles(Set.of(adminRole));

        given(userRepository.findByIdAndDeletedAtIsNull(userId))
                .willReturn(Optional.of(user));
    }

    public static String userAccessToken(JwtService jwtService, long userId) {
        return jwtService.issueAccessToken(
                        userId,
                        "user",
                        "user@example.com",
                        true,
                        List.of(RbacRoles.USER),
                        Set.of(),
                        Map.of()
                )
                .accessToken();
    }

    public static void stubEnabledUser(UserRepository userRepository, long userId) {
        RoleEntity userRole = new RoleEntity();
        userRole.setName(RbacRoles.USER);
        userRole.setEnabled(true);

        userRole.setPermissions(Set.of(
                permission(RbacPermissions.API_DOCS_ACCESS),
                permission(RbacPermissions.API_TEST_EXECUTE)
        ));

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("user");
        user.setEmail("user@example.com");
        user.setEnabled(true);
        user.setRoles(Set.of(userRole));

        given(userRepository.findByIdAndDeletedAtIsNull(userId))
                .willReturn(Optional.of(user));
    }

    public static String viewerAccessToken(JwtService jwtService, long userId) {
        return jwtService.issueAccessToken(
                        userId,
                        "viewer",
                        "viewer@example.com",
                        true,
                        List.of(RbacRoles.VIEWER),
                        Set.of(),
                        Map.of()
                )
                .accessToken();
    }

    public static void stubEnabledViewer(UserRepository userRepository, long userId) {
        RoleEntity viewerRole = new RoleEntity();
        viewerRole.setName(RbacRoles.VIEWER);
        viewerRole.setEnabled(true);

        viewerRole.setPermissions(Set.of(
                permission(RbacPermissions.API_DOCS_ACCESS)
        ));

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("viewer");
        user.setEmail("viewer@example.com");
        user.setEnabled(true);
        user.setRoles(Set.of(viewerRole));

        given(userRepository.findByIdAndDeletedAtIsNull(userId))
                .willReturn(Optional.of(user));
    }

    private static PermissionEntity permission(String permissionName) {
        PermissionEntity p = new PermissionEntity();
        p.setName(permissionName);
        p.setEnabled(true);
        p.setDeletedAt(null);
        return p;
    }
}
