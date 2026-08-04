package com.specpulse.settings.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_settings")
@Getter
@Setter
public class ApplicationSetting {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "setting_key", nullable = false, length = 255)
    private String key;

    @Type(JsonType.class)
    @Column(name = "setting_value", columnDefinition = "jsonb")
    private Object value;

    @Column(name = "value_type", nullable = false, length = 50)
    private String valueType = "string";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "is_editable", nullable = false)
    private Boolean isEditable = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Get the value as a string
     */
    public String getValueAsString() {
        if (value == null) {
            return null;
        }

        if (value instanceof String s) {
            // If value is stored as a JSON string literal (e.g. "foo"), return the unquoted value.
            if (looksLikeJsonStringLiteral(s)) {
                try {
                    return OBJECT_MAPPER.readValue(s, String.class);
                } catch (Exception ignored) {
                    // Fall back to raw string.
                }
            }
        }
        return value.toString();
    }

    /**
     * Get the value as a boolean
     */
    public Boolean getValueAsBoolean() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String s = value.toString();
            try {
                // Handle both JSON booleans (true/false) and JSON string literals (e.g. "true").
                if (s != null) {
                    if (looksLikeJsonStringLiteral(s)) {
                        String unquoted = OBJECT_MAPPER.readValue(s, String.class);
                        return Boolean.parseBoolean(unquoted);
                    }
                    return OBJECT_MAPPER.readValue(s, Boolean.class);
                }
            } catch (Exception ignored) {
                // Fall back to Boolean.parseBoolean.
            }
            return Boolean.parseBoolean(s);
        }
        return null;
    }

    /**
     * Get the value as an integer
     */
    public Integer getValueAsInteger() {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            String s = value.toString();
            try {
                // Handle JSON number token (123)
                return OBJECT_MAPPER.readValue(s, Integer.class);
            } catch (Exception ignored) {
                // Handle JSON string literal (e.g. "123")
                if (looksLikeJsonStringLiteral(s)) {
                    try {
                        String unquoted = OBJECT_MAPPER.readValue(s, String.class);
                        return Integer.parseInt(unquoted);
                    } catch (Exception ignored2) {
                        return null;
                    }
                }
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean looksLikeJsonStringLiteral(String s) {
        if (s == null) return false;
        String t = s.trim();
        return t.startsWith("\"") && t.endsWith("\"");
    }
}
