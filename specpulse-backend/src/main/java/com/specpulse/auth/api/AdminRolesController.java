package com.specpulse.auth.api;

import com.specpulse.auth.application.UserAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
public class AdminRolesController {

    private final UserAdminService userAdminService;

    public AdminRolesController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<List<UserAdminService.RoleDto>> listEnabled() {
        return ResponseEntity.ok(userAdminService.listEnabledRoles());
    }
}
