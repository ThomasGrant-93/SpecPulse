package com.specpulse.settings;

import com.specpulse.api.mapper.ApplicationSettingApiMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@Slf4j
@Tag(name = "Application Settings", description = "API для управления настройками приложения")
@Validated
public class ApplicationSettingController {

    private final ApplicationSettingService settingService;
    private final ApplicationSettingApiMapper settingApiMapper;

    // If configured (non-empty), requires Authorization: Bearer <token> for settings mutations.
    private final String authToken;

    public ApplicationSettingController(
            ApplicationSettingService settingService,
            ApplicationSettingApiMapper settingApiMapper,
            @Value("${specpulse.auth.token:}") String authToken
    ) {
        this.settingService = settingService;
        this.settingApiMapper = settingApiMapper;
        this.authToken = authToken;
    }

    private boolean isSettingsAuthorized(String authorization) {
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
    @Operation(summary = "Получить все настройки", description = "Возвращает все настройки сгруппированные по категориям")
    public ResponseEntity<List<ApplicationSettingDTO.SettingsCategory>> getAllSettings() {
        return ResponseEntity.ok(settingService.getAllSettingsGrouped());
    }

    @GetMapping("/public")
    @Operation(summary = "Получить публичные настройки", description = "Возвращает только публичные настройки (доступно без аутентификации)")
    public ResponseEntity<List<ApplicationSettingDTO.SettingsCategory>> getPublicSettings() {
        return ResponseEntity.ok(settingService.getPublicSettingsGrouped());
    }

    @GetMapping("/{category}")
    @Operation(summary = "Получить настройки категории", description = "Возвращает все настройки указанной категории")
    public ResponseEntity<ApplicationSettingDTO.SettingsCategory> getCategorySettings(
            @Parameter(description = "Категория настроек") @PathVariable String category) {
        return ResponseEntity.ok(settingService.getCategorySettings(category));
    }

    @GetMapping("/{category}/{key}")
    @Operation(summary = "Получить конкретную настройку", description = "Возвращает настройку по категории и ключу")
    public ResponseEntity<ApplicationSettingDTO> getSetting(
            @Parameter(description = "Категория") @PathVariable String category,
            @Parameter(description = "Ключ") @PathVariable String key) {
        return ResponseEntity.ok(settingService.getSetting(category, key));
    }

    @PutMapping("/{category}/{key}")
    @Operation(summary = "Обновить настройку", description = "Обновляет значение указанной настройки")
    public ResponseEntity<ApplicationSettingDTO> updateSetting(
            @Parameter(description = "Категория")
            @PathVariable @Pattern(regexp = "^[a-z_]+$", message = "Category must contain only lowercase letters and underscores") String category,
            @Parameter(description = "Ключ")
            @PathVariable @Pattern(regexp = "^[a-z0-9_.]+$", message = "Key must contain only lowercase letters, numbers, underscores and dots") String key,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody SettingUpdateRequest request) {
        log.info("Updating setting: {}.{}", category, key);
        if (!isSettingsAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(settingService.updateSetting(category, key, request.getValue()));
    }

    @PatchMapping
    @Operation(summary = "Массовое обновление настроек", description = "Обновляет несколько настроек за один запрос")
    public ResponseEntity<List<ApplicationSettingDTO>> updateSettings(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> updates) {
        log.info("Bulk updating {} settings", updates.size());
        if (!isSettingsAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var mappedUpdates = settingApiMapper.toBulkUpdateCommands(updates);

        if (!mappedUpdates.invalidKeys().isEmpty()) {
            log.warn("Skipped {} invalid setting keys: {}",
                    mappedUpdates.invalidKeys().size(), mappedUpdates.invalidKeys());
        }

        return ResponseEntity.ok(settingService.updateSettings(mappedUpdates.commands()));
    }

    @GetMapping("/categories")
    @Operation(summary = "Получить список категорий", description = "Возвращает все доступные категории настроек")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(settingService.getAllSettingsGrouped().stream()
                .map(ApplicationSettingDTO.SettingsCategory::getName)
                .toList());
    }

    /**
     * DTO для валидации обновления настройки
     */
    @Data
    public static class SettingUpdateRequest {
        @Parameter(description = "Значение настройки", required = true)
        private Object value;
    }
}
