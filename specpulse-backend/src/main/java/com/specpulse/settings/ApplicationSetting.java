package com.specpulse.settings;

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
     * Получить значение как строку
     */
    public String getValueAsString() {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Получить значение как boolean
     */
    public Boolean getValueAsBoolean() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean(value.toString());
        }
        return null;
    }

    /**
     * Получить значение как Integer
     */
    public Integer getValueAsInteger() {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
