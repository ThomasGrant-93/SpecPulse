package com.specpulse.history;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "spec_version_id")
    private Long specVersionId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_details", columnDefinition = "TEXT")
    private String eventDetails;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Constructors
    public AuditLogEntity() {
    }

    public AuditLogEntity(Long serviceId, Long specVersionId, String eventType, String eventDetails) {
        this.serviceId = serviceId;
        this.specVersionId = specVersionId;
        this.eventType = eventType;
        this.eventDetails = eventDetails;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getSpecVersionId() {
        return specVersionId;
    }

    public void setSpecVersionId(Long specVersionId) {
        this.specVersionId = specVersionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventDetails() {
        return eventDetails;
    }

    public void setEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
