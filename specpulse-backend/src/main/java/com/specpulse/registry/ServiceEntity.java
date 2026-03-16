package com.specpulse.registry;

import com.specpulse.entity.ServiceGroup;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "services")
@org.hibernate.annotations.SQLDelete(sql = "UPDATE services SET enabled = false WHERE id = ?")
@org.hibernate.annotations.Where(clause = "enabled = true")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String openApiUrl;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ServiceGroup group;

    // search_vector is managed by database trigger, not JPA
    // Exclude from INSERT/UPDATE statements
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;

    // Constructors
    public ServiceEntity() {
    }

    public ServiceEntity(String name, String openApiUrl, String description) {
        this.name = name;
        this.openApiUrl = openApiUrl;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOpenApiUrl() {
        return openApiUrl;
    }

    public void setOpenApiUrl(String openApiUrl) {
        this.openApiUrl = openApiUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // searchVector getter - managed by DB trigger
    public String getSearchVector() {
        return searchVector;
    }

    public ServiceGroup getGroup() {
        return group;
    }

    public void setGroup(ServiceGroup group) {
        this.group = group;
    }
}
