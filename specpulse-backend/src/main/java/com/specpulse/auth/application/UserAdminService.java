package com.specpulse.auth.application;

import com.specpulse.auth.domain.RoleEntity;
import com.specpulse.auth.domain.UserEntity;
import com.specpulse.auth.infrastructure.RoleRepository;
import com.specpulse.auth.infrastructure.UserRepository;
import com.specpulse.exception.DuplicateResourceException;
import com.specpulse.exception.ResourceNotFoundException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return userRepository.findAllByDeletedAtIsNull().stream()
                .map(UserDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.findByUsernameAndDeletedAtIsNull(request.username()).isPresent()) {
            throw new DuplicateResourceException("User with username '" + request.username() + "' already exists");
        }
        if (userRepository.findByEmailAndDeletedAtIsNull(request.email()).isPresent()) {
            throw new DuplicateResourceException("User with email '" + request.email() + "' already exists");
        }

        List<RoleEntity> roles = resolveRoles(request.roleNames());

        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(request.enabled());
        user.setRoles(new HashSet<>(roles));

        return UserDto.fromEntity(userRepository.save(user));
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (!Objects.equals(user.getEmail(), request.email())) {
            userRepository.findByEmailAndDeletedAtIsNull(request.email())
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(ignored -> {
                        throw new DuplicateResourceException("User with email '" + request.email() + "' already exists");
                    });
        }

        List<RoleEntity> roles = resolveRoles(request.roleNames());

        user.setEmail(request.email());
        user.setEnabled(request.enabled());
        user.setRoles(new HashSet<>(roles));

        return UserDto.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        user.setEnabled(false);
        user.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listEnabledRoles() {
        return roleRepository.findAllByEnabledTrueAndDeletedAtIsNull().stream()
                .map(RoleDto::fromEntity)
                .toList();
    }

    private List<RoleEntity> resolveRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }

        List<RoleEntity> roles = roleRepository.findByNameInAndEnabledTrueAndDeletedAtIsNull(roleNames);

        // Best-effort validation: unknown roles should fail fast.
        if (roles.size() != new HashSet<>(roleNames).size()) {
            throw new IllegalArgumentException("One or more roles are invalid");
        }

        return roles;
    }

    public record UserDto(
            Long id,
            String username,
            String email,
            boolean enabled,
            List<String> roles
    ) {
        static UserDto fromEntity(UserEntity user) {
            List<String> roleNames = user.getRoles().stream()
                    .filter(r -> r.getDeletedAt() == null && r.isEnabled())
                    .map(RoleEntity::getName)
                    .toList();

            return new UserDto(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.isEnabled(),
                    roleNames
            );
        }
    }

    public record RoleDto(
            Long id,
            String name,
            boolean enabled
    ) {
        static RoleDto fromEntity(RoleEntity role) {
            return new RoleDto(role.getId(), role.getName(), role.isEnabled());
        }
    }

    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank @Email String email,
            @NotBlank String password,
            boolean enabled,
            @NotNull List<String> roleNames
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank @Email String email,
            boolean enabled,
            @NotNull List<String> roleNames
    ) {
    }
}
