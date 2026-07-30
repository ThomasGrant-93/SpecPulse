package com.specpulse.settings;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ApplicationSettingRepositoryPort settingRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get all settings grouped by categories
     */
    public List<ApplicationSettingDTO.SettingsCategory> getAllSettingsGrouped() {
        List<ApplicationSetting> allSettings = settingRepository.findAllByOrderByCategoryAscKeyAsc();
        return groupByCategory(allSettings);
    }

    /**
     * Get category settings
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
     * Get public settings (for a frontend without authentication)
     */
    public List<ApplicationSettingDTO.SettingsCategory> getPublicSettingsGrouped() {
        List<ApplicationSetting> allSettings = settingRepository.findAllByOrderByCategoryAscKeyAsc();
        List<ApplicationSetting> publicSettings = allSettings.stream()
                .filter(ApplicationSetting::getIsPublic)
                .collect(Collectors.toList());

        return groupByCategory(publicSettings);
    }

    /**
     * Get a specific setting
     */
    public ApplicationSettingDTO getSetting(String category, String key) {
        ApplicationSetting setting = settingRepository.findByCategoryAndKey(category, key)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Setting not found: %s.%s", category, key)));
        return toDTO(setting);
    }

    /**
     * Update a setting
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

        // Ensure we always write valid JSON into jsonb column.
        // Hypersistence JsonType stores Java strings as-is into jsonb, so we must provide JSON literals.
        Object jsonbValue = serializeValueForJsonb(value, setting.getValueType());
        setting.setValue(jsonbValue);

        ApplicationSetting updated = settingRepository.save(setting);
        log.info("Updated setting: {}.{} = {}", category, key, value);

        return toDTO(updated);
    }

    /**
     * Bulk update settings
     */
    @Transactional
    public List<ApplicationSettingDTO> updateSettings(List<SettingUpdateCommand> updates) {
        List<ApplicationSettingDTO> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (SettingUpdateCommand update : updates) {
            try {
                ApplicationSettingDTO result = updateSetting(update.category(), update.key(), update.value());
                results.add(result);
            } catch (NoSuchElementException e) {
                String errorMsg = String.format("Setting not found: %s", update.fullKey());
                log.error(errorMsg, e);
                errors.add(errorMsg);
            } catch (IllegalStateException e) {
                String errorMsg = String.format("Setting not editable: %s", update.fullKey());
                log.error(errorMsg, e);
                errors.add(errorMsg);
            } catch (RuntimeException e) {
                String errorMsg = String.format("Failed to update setting %s: %s", update.fullKey(), e.getMessage());
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
     * Get a setting value (convenience method)
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
                .value(normalizeValueForApi(setting.getValue(), setting.getValueType()))
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

    private Object serializeValueForJsonb(Object value, String valueType) {
        if (value == null) {
            return null;
        }

        String normalizedType = valueType == null ? "string" : valueType.toLowerCase();

        try {
            return switch (normalizedType) {
                case "boolean" -> {
                    Boolean b = coerceBoolean(value);
                    yield objectMapper.writeValueAsString(b); // => true/false (JSON literal)
                }
                case "integer" -> {
                    Integer i = coerceInteger(value);
                    yield objectMapper.writeValueAsString(i); // => 123
                }
                case "number" -> {
                    // Best-effort numeric support for non-integer values.
                    if (value instanceof Number n) {
                        yield objectMapper.writeValueAsString(n);
                    }
                    if (value instanceof String s) {
                        JsonNode node = objectMapper.readTree(s);
                        if (!node.isNumber()) {
                            throw new IllegalArgumentException("Invalid numeric setting value");
                        }
                        yield objectMapper.writeValueAsString(node.numberValue());
                    }
                    yield objectMapper.writeValueAsString(value);
                }
                case "array" -> {
                    Object arrayValue = (value instanceof List<?>) ? value : Collections.singletonList(value);
                    yield objectMapper.writeValueAsString(arrayValue);
                }
                case "object", "json" -> objectMapper.writeValueAsString(value);
                case "string" -> {
                    if (!(value instanceof String s)) {
                        throw new IllegalArgumentException(
                                "Invalid value type for settings valueType=string. Expected a string." +
                                        " Received: " + value.getClass().getSimpleName());
                    }
                    yield objectMapper.writeValueAsString(s); // => "foo"
                }
                default -> {
                    // For unknown types we treat input as a string unless it's already valid JSON.
                    if (value instanceof String s) {
                        if (looksLikeJson(s)) {
                            // Store raw JSON literal as-is.
                            yield s;
                        }
                        yield objectMapper.writeValueAsString(s);
                    }
                    yield objectMapper.writeValueAsString(value);
                }
            };
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize setting value as JSON");
        }
    }

    private Object normalizeValueForApi(Object storedValue, String valueType) {
        if (storedValue == null) {
            return null;
        }

        String normalizedType = valueType == null ? "string" : valueType.toLowerCase();

        try {
            return switch (normalizedType) {
                case "boolean" -> {
                    if (storedValue instanceof Boolean b) yield b;
                    if (storedValue instanceof String s) yield objectMapper.readValue(s, Boolean.class);
                    yield Boolean.parseBoolean(storedValue.toString());
                }
                case "integer" -> {
                    if (storedValue instanceof Integer i) yield i;
                    if (storedValue instanceof Number n) yield n.intValue();
                    if (storedValue instanceof String s) yield objectMapper.readValue(s, Integer.class);
                    yield Integer.parseInt(storedValue.toString());
                }
                case "number" -> {
                    if (storedValue instanceof Number n) yield n;
                    if (storedValue instanceof String s) {
                        JsonNode node = objectMapper.readTree(s);
                        yield node.numberValue();
                    }
                    yield Double.parseDouble(storedValue.toString());
                }
                case "array" -> {
                    if (storedValue instanceof List<?> l) yield l;
                    if (storedValue instanceof String s) {
                        yield objectMapper.readValue(s, new TypeReference<List<Object>>() {
                        });
                    }
                    yield storedValue;
                }
                case "object", "json" -> {
                    if (storedValue instanceof java.util.Map<?, ?> m) yield storedValue;
                    if (storedValue instanceof String s) {
                        yield objectMapper.readValue(s, new TypeReference<java.util.Map<String, Object>>() {
                        });
                    }
                    yield storedValue;
                }
                case "string" -> {
                    if (storedValue instanceof String s) {
                        // If stored as JSON string literal (e.g. "foo"), unquote it.
                        if (looksLikeJsonStringLiteral(s)) {
                            yield objectMapper.readValue(s, String.class);
                        }
                        yield s;
                    }
                    yield storedValue.toString();
                }
                default -> storedValue;
            };
        } catch (Exception e) {
            // API should not leak raw JSON parsing errors.
            log.warn("Failed to normalize setting value for API, key={}.{} type={}. Error={}",
                    settingCategoryPlaceholder(), settingKeyPlaceholder(), valueType, e.getMessage());
            return storedValue;
        }
    }

    private String settingCategoryPlaceholder() {
        return "?";
    }

    private String settingKeyPlaceholder() {
        return "?";
    }

    private Boolean coerceBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            String normalized = s.trim();
            if (normalized.equalsIgnoreCase("true")) return true;
            if (normalized.equalsIgnoreCase("false")) return false;
        }
        throw new IllegalArgumentException("Invalid boolean setting value");
    }

    private Integer coerceInteger(Object value) {
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("Invalid integer setting value");
            }
        }
        throw new IllegalArgumentException("Invalid integer setting value");
    }

    private boolean looksLikeJson(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.startsWith("{") || t.startsWith("[") || t.startsWith("\"") || t.equals("null") || t.equals("true") || t.equals("false");
    }

    private boolean looksLikeJsonStringLiteral(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.startsWith("\"") && t.endsWith("\"");
    }
}
