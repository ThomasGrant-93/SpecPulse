package com.specpulse.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ApplicationSettingService {

    private final ApplicationSettingRepository settingRepository;

    /**
     * Получить все настройки сгруппированные по категориям
     */
    public List<ApplicationSettingDTO.SettingsCategory> getAllSettingsGrouped() {
        List<ApplicationSetting> allSettings = settingRepository.findAllByOrderByCategoryAscKeyAsc();
        return groupByCategory(allSettings);
    }

    /**
     * Получить настройки категории
     */
    public ApplicationSettingDTO.SettingsCategory getCategorySettings(String category) {
        List<ApplicationSetting> settings = settingRepository.findByCategoryOrderByKeyAsc(category);

        if (settings.isEmpty()) {
            throw new NoSuchElementException("Settings category not found: " + category);
        }

        List<ApplicationSettingDTO> dtos = settings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ApplicationSettingDTO.SettingsCategory.builder()
                .name(category)
                .displayName(formatCategoryName(category))
                .settings(dtos)
                .build();
    }

    /**
     * Получить публичные настройки (для frontend без аутентификации)
     */
    public List<ApplicationSettingDTO.SettingsCategory> getPublicSettingsGrouped() {
        List<ApplicationSetting> allSettings = settingRepository.findAllByOrderByCategoryAscKeyAsc();
        List<ApplicationSetting> publicSettings = allSettings.stream()
                .filter(ApplicationSetting::getIsPublic)
                .collect(Collectors.toList());

        return groupByCategory(publicSettings);
    }

    /**
     * Получить конкретную настройку
     */
    public ApplicationSettingDTO getSetting(String category, String key) {
        ApplicationSetting setting = settingRepository.findByCategoryAndKey(category, key)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Setting not found: %s.%s", category, key)));
        return toDTO(setting);
    }

    /**
     * Обновить настройку
     */
    @Transactional
    public ApplicationSettingDTO updateSetting(String category, String key, Object value) {
        ApplicationSetting setting = settingRepository.findByCategoryAndKey(category, key)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Setting not found: %s.%s", category, key)));

        if (!setting.getIsEditable()) {
            throw new IllegalStateException(
                    String.format("Setting is not editable: %s.%s", category, key));
        }

        // Валидация типа значения
        Object typedValue = validateAndConvertType(value, setting.getValueType());
        setting.setValue(typedValue);

        ApplicationSetting updated = settingRepository.save(setting);
        log.info("Updated setting: {}.{} = {}", category, key, value);

        return toDTO(updated);
    }

    /**
     * Массовое обновление настроек
     */
    @Transactional
    public List<ApplicationSettingDTO> updateSettings(Map<String, Object> updates) {
        List<ApplicationSettingDTO> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String[] parts = entry.getKey().split("\\.", 2);
            if (parts.length != 2) {
                String errorMsg = String.format("Invalid setting key format: %s", entry.getKey());
                log.error(errorMsg);
                errors.add(errorMsg);
                continue;
            }

            try {
                ApplicationSettingDTO result = updateSetting(parts[0], parts[1], entry.getValue());
                results.add(result);
            } catch (NoSuchElementException e) {
                String errorMsg = String.format("Setting not found: %s", entry.getKey());
                log.error(errorMsg, e);
                errors.add(errorMsg);
            } catch (IllegalStateException e) {
                String errorMsg = String.format("Setting not editable: %s", entry.getKey());
                log.error(errorMsg, e);
                errors.add(errorMsg);
            } catch (RuntimeException e) {
                String errorMsg = String.format("Failed to update setting %s: %s", entry.getKey(), e.getMessage());
                log.error(errorMsg, e);
                errors.add(errorMsg);
            }
        }

        if (!errors.isEmpty()) {
            log.warn("Bulk update completed with {} errors: {}", errors.size(), errors);
        }

        return results;
    }

    /**
     * Получить значение настройки (удобный метод)
     */
    public <T> T getSettingValue(String category, String key, Class<T> type) {
        ApplicationSetting setting = settingRepository.findByCategoryAndKey(category, key)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Setting not found: %s.%s", category, key)));

        if (type == String.class) {
            return type.cast(setting.getValueAsString());
        } else if (type == Boolean.class) {
            return type.cast(setting.getValueAsBoolean());
        } else if (type == Integer.class) {
            return type.cast(setting.getValueAsInteger());
        }

        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    // ========== Private methods ==========

    private List<ApplicationSettingDTO.SettingsCategory> groupByCategory(List<ApplicationSetting> settings) {
        Map<String, List<ApplicationSetting>> grouped = settings.stream()
                .collect(Collectors.groupingBy(ApplicationSetting::getCategory));

        return grouped.entrySet().stream()
                .map(entry -> ApplicationSettingDTO.SettingsCategory.builder()
                        .name(entry.getKey())
                        .displayName(formatCategoryName(entry.getKey()))
                        .description(getCategoryDescription(entry.getKey()))
                        .settings(entry.getValue().stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList()))
                        .build())
                .sorted(Comparator.comparing(ApplicationSettingDTO.SettingsCategory::getName))
                .collect(Collectors.toList());
    }

    private ApplicationSettingDTO toDTO(ApplicationSetting setting) {
        return ApplicationSettingDTO.builder()
                .id(setting.getId())
                .category(setting.getCategory())
                .key(setting.getKey())
                .value(setting.getValue())
                .valueType(setting.getValueType())
                .description(setting.getDescription())
                .isPublic(setting.getIsPublic())
                .isEditable(setting.getIsEditable())
                .createdAt(setting.getCreatedAt() != null ? setting.getCreatedAt() : null)
                .updatedAt(setting.getUpdatedAt() != null ? setting.getUpdatedAt() : null)
                .build();
    }

    private String formatCategoryName(String category) {
        return Arrays.stream(category.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String getCategoryDescription(String category) {
        Map<String, String> descriptions = Map.of(
                "general", "Общие настройки приложения",
                "scheduler", "Настройки планировщика",
                "pull", "Настройки pull сервиса",
                "notifications", "Настройки уведомлений",
                "security", "Настройки безопасности",
                "audit", "Настройки аудита"
        );
        return descriptions.getOrDefault(category, "Настройки");
    }

    private Object validateAndConvertType(Object value, String valueType) {
        return switch (valueType) {
            case "boolean" -> Boolean.parseBoolean(value.toString());
            case "integer" -> Integer.parseInt(value.toString());
            case "array" -> {
                if (value instanceof List) {
                    yield value;
                }
                yield Collections.singletonList(value);
            }
            default -> value.toString();
        };
    }
}
