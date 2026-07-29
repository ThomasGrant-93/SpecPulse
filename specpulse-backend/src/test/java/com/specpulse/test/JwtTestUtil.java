package com.specpulse.test;

import com.specpulse.auth.entity.RoleEntity;
import com.specpulse.auth.entity.UserEntity;
import com.specpulse.auth.repository.UserRepository;
import com.specpulse.auth.security.JwtService;

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
                        List.of("ADMIN"),
                        Set.of(),
                        Map.of()
                )
                .accessToken();
    }

    public static void stubEnabledAdmin(UserRepository userRepository, long userId) {
        RoleEntity adminRole = new RoleEntity();
        adminRole.setName("ADMIN");
        adminRole.setEnabled(true);

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setEnabled(true);
        user.setRoles(Set.of(adminRole));

        given(userRepository.findByIdAndDeletedAtIsNull(userId))
                .willReturn(Optional.of(user));
    }
}
