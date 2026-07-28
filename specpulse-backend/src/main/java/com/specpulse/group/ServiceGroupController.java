package com.specpulse.group;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Service Groups", description = "API для управления группами сервисов")
public class ServiceGroupController {

    private final ServiceGroupService groupService;

    // If configured (non-empty), requires Authorization: Bearer <token> for group mutations.
    @Value("${specpulse.auth.token:}")
    private String authToken;

    private boolean isGroupAuthorized(String authorization) {
        if (authToken == null || authToken.isBlank()) {
            return true; // Backward-compatible default.
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        return authToken.equals(token);
    }

    @GetMapping
    @Operation(summary = "Получить все группы", description = "Возвращает иерархическую структуру всех групп")
    public ResponseEntity<List<ServiceGroupDTO>> getAllGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/root")
    @Operation(summary = "Получить корневые группы", description = "Возвращает только корневые группы (без родителя)")
    public ResponseEntity<List<ServiceGroupDTO>> getRootGroups() {
        return ResponseEntity.ok(groupService.getRootGroups());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить группу по ID", description = "Возвращает детальную информацию о группе")
    public ResponseEntity<ServiceGroupDTO> getGroupById(
            @Parameter(description = "ID группы") @PathVariable Long id,
            @Parameter(description = "Включить сервисы группы")
            @RequestParam(defaultValue = "false") boolean includeServices) {
        return ResponseEntity.ok(groupService.getGroupById(id, includeServices));
    }

    @PostMapping
    @Operation(summary = "Создать группу", description = "Создает новую группу сервисов")
    public ResponseEntity<ServiceGroupDTO> createGroup(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateGroupRequest request) {
        if (!isGroupAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Creating group: {}", request.getName());
        ServiceGroupDTO created = groupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить группу", description = "Обновляет существующую группу")
    public ResponseEntity<ServiceGroupDTO> updateGroup(
            @Parameter(description = "ID группы") @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody UpdateGroupRequest request) {
        if (!isGroupAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Updating group: {}", id);
        return ResponseEntity.ok(groupService.updateGroup(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить группу", description = "Удаляет группу (только если нет подгрупп)")
    public ResponseEntity<Void> deleteGroup(
            @Parameter(description = "ID группы") @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        if (!isGroupAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Deleting group: {}", id);
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/services")
    @Operation(summary = "Добавить сервисы в группу", description = "Добавляет указанные сервисы в группу")
    public ResponseEntity<ServiceGroupDTO> addServicesToGroup(
            @Parameter(description = "ID группы") @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Parameter(description = "Список ID сервисов") @RequestBody List<Long> serviceIds) {
        if (!isGroupAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Adding {} services to group: {}", serviceIds.size(), id);
        return ResponseEntity.ok(groupService.addServicesToGroup(id, serviceIds));
    }

    @DeleteMapping("/{id}/services/{serviceId}")
    @Operation(summary = "Удалить сервис из группы", description = "Удаляет сервис из группы")
    public ResponseEntity<Void> removeServiceFromGroup(
            @Parameter(description = "ID группы") @PathVariable Long id,
            @Parameter(description = "ID сервиса") @PathVariable Long serviceId,
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        if (!isGroupAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Removing service {} from group {}", serviceId, id);
        groupService.removeServiceFromGroup(id, serviceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/services/{serviceId}")
    @Operation(summary = "Получить группы сервиса", description = "Возвращает все группы, в которых состоит сервис")
    public ResponseEntity<List<ServiceGroupDTO>> getServiceGroups(
            @Parameter(description = "ID сервиса") @PathVariable Long serviceId) {
        return ResponseEntity.ok(groupService.getServiceGroups(serviceId));
    }
}
