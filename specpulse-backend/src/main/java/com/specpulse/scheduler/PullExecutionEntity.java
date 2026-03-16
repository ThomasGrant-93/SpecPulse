package com.specpulse.scheduler;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pull_executions")
public class PullExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(nullable = false)
    private String status;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "new_version_created")
    private Boolean newVersionCreated = false;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    // Constructors
    public PullExecutionEntity() {
    }

    public PullExecutionEntity(Long serviceId, String status, Instant executedAt) {
        this.serviceId = serviceId;
        this.status = status;
        this.executedAt = executedAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Boolean getNewVersionCreated() {
        return newVersionCreated;
    }

    public void setNewVersionCreated(Boolean newVersionCreated) {
        this.newVersionCreated = newVersionCreated;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }
}
