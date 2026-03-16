package com.specpulse.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Application Settings", description = "API для управления настройками приложения")
@Validated
public class ApplicationSettingController {

    private final ApplicationSettingService settingService;

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
            @Valid @RequestBody SettingUpdateRequest request) {
        log.info("Updating setting: {}.{}", category, key);
        return ResponseEntity.ok(settingService.updateSetting(category, key, request.getValue()));
    }

    @PatchMapping
    @Operation(summary = "Массовое обновление настроек", description = "Обновляет несколько настроек за один запрос")
    public ResponseEntity<List<ApplicationSettingDTO>> updateSettings(
            @RequestBody Map<String, Object> updates) {
        log.info("Bulk updating {} settings", updates.size());
        return ResponseEntity.ok(settingService.updateSettings(updates));
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
