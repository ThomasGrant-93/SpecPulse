package com.specpulse.auth.api;

import com.specpulse.auth.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUsersController {

    private final UserAdminService userAdminService;

    public AdminUsersController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<List<UserAdminService.UserDto>> list() {
        return ResponseEntity.ok(userAdminService.listUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAdminService.UserDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.getUser(id));
    }

    @PostMapping
    public ResponseEntity<UserAdminService.UserDto> create(
            @Valid @RequestBody UserAdminService.CreateUserRequest request
    ) {
        var created = userAdminService.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/users/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAdminService.UserDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UserAdminService.UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userAdminService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userAdminService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
