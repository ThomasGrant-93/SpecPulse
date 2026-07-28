package com.specpulse.diff;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "spec_diffs")
public class SpecDiffEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "from_version_id", nullable = false)
    private Long fromVersionId;

    @Column(name = "to_version_id", nullable = false)
    private Long toVersionId;

    @Column(name = "diff_content", nullable = false, columnDefinition = "TEXT")
    private String diffContent;

    @Column(name = "has_breaking_changes", nullable = false)
    private boolean hasBreakingChanges = false;

    @Column(name = "breaking_changes_count", nullable = false)
    private int breakingChangesCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Constructors
    public SpecDiffEntity() {
    }

    public SpecDiffEntity(Long serviceId, Long fromVersionId, Long toVersionId,
                          String diffContent, boolean hasBreakingChanges, int breakingChangesCount) {
        this.serviceId = serviceId;
        this.fromVersionId = fromVersionId;
        this.toVersionId = toVersionId;
        this.diffContent = diffContent;
        this.hasBreakingChanges = hasBreakingChanges;
        this.breakingChangesCount = breakingChangesCount;
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

    public Long getFromVersionId() {
        return fromVersionId;
    }

    public void setFromVersionId(Long fromVersionId) {
        this.fromVersionId = fromVersionId;
    }

    public Long getToVersionId() {
        return toVersionId;
    }

    public void setToVersionId(Long toVersionId) {
        this.toVersionId = toVersionId;
    }

    public String getDiffContent() {
        return diffContent;
    }

    public void setDiffContent(String diffContent) {
        this.diffContent = diffContent;
    }

    public boolean isHasBreakingChanges() {
        return hasBreakingChanges;
    }

    public void setHasBreakingChanges(boolean hasBreakingChanges) {
        this.hasBreakingChanges = hasBreakingChanges;
    }

    public int getBreakingChangesCount() {
        return breakingChangesCount;
    }

    public void setBreakingChangesCount(int breakingChangesCount) {
        this.breakingChangesCount = breakingChangesCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
