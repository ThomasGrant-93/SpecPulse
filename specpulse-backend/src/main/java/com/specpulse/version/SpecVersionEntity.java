package com.specpulse.version;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;

@Entity
@Table(name = "spec_versions")
public class SpecVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private com.specpulse.registry.ServiceEntity service;

    @Column(name = "version_hash", nullable = false, length = 64)
    private String versionHash;

    @Type(JsonBinaryType.class)
    @Column(name = "spec_content", nullable = false, columnDefinition = "jsonb")
    private com.fasterxml.jackson.databind.JsonNode specContent;

    @Column(name = "spec_version", length = 50)
    private String specVersion;

    @Column(name = "spec_title", length = 500)
    private String specTitle;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "pulled_at", nullable = false)
    private Instant pulledAt;

    // Constructors
    public SpecVersionEntity() {
    }

    public SpecVersionEntity(com.specpulse.registry.ServiceEntity service,
                             String versionHash,
                             com.fasterxml.jackson.databind.JsonNode specContent,
                             String specVersion,
                             String specTitle) {
        this.service = service;
        this.versionHash = versionHash;
        this.specContent = specContent;
        this.specVersion = specVersion;
        this.specTitle = specTitle;
        this.fileSizeBytes = specContent != null ? (long) specContent.toString().length() : 0L;
        this.pulledAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public com.specpulse.registry.ServiceEntity getService() {
        return service;
    }

    public void setService(com.specpulse.registry.ServiceEntity service) {
        this.service = service;
    }

    public String getVersionHash() {
        return versionHash;
    }

    public void setVersionHash(String versionHash) {
        this.versionHash = versionHash;
    }

    public com.fasterxml.jackson.databind.JsonNode getSpecContent() {
        return specContent;
    }

    public void setSpecContent(com.fasterxml.jackson.databind.JsonNode specContent) {
        this.specContent = specContent;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(String specVersion) {
        this.specVersion = specVersion;
    }

    public String getSpecTitle() {
        return specTitle;
    }

    public void setSpecTitle(String specTitle) {
        this.specTitle = specTitle;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public Instant getPulledAt() {
        return pulledAt;
    }

    public void setPulledAt(Instant pulledAt) {
        this.pulledAt = pulledAt;
    }
}
