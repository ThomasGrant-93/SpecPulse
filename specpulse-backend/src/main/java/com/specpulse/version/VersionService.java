package com.specpulse.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.parser.OpenApiParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class VersionService {

    private final SpecVersionRepository repository;
    private final OpenApiParser parser;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SpecVersionDTO> getVersionsByServiceId(Long serviceId) {
        return repository.findByServiceIdOrderByPulledAtDesc(serviceId).stream()
                .map(SpecVersionDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<SpecVersionDTO> getLatestVersion(Long serviceId) {
        return repository.findFirstByServiceIdOrderByPulledAtDesc(serviceId)
                .map(SpecVersionDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Optional<SpecVersionEntity> getLatestVersionEntity(Long serviceId) {
        return repository.findFirstByServiceIdOrderByPulledAtDesc(serviceId);
    }

    /**
     * Pull OpenAPI spec from service and save if changed
     *
     * @return PullResult with information about changes
     */
    @Transactional
    public PullResult pullAndSaveVersion(Long serviceId, String serviceName, String specContent) {
        OpenApiParser.ParseResult parseResult = parser.parse(specContent);

        if (!parseResult.success()) {
            throw new IllegalArgumentException("Failed to parse OpenAPI spec: " + parseResult.errorMessage());
        }

        String newHash = parseResult.contentHash();

        // Get latest version to compare hash
        Optional<SpecVersionEntity> latestVersion = repository.findFirstByServiceIdOrderByPulledAtDesc(serviceId);

        // Check if version with same hash already exists
        if (latestVersion.isPresent() && latestVersion.get().getVersionHash().equals(newHash)) {
            log.info("No changes detected for service {} (hash: {})", serviceName, newHash);
            return PullResult.unchanged(latestVersion.get().getId(), newHash);
        }

        // Create a minimal service entity reference for saving
        SpecVersionEntity entity = new SpecVersionEntity();
        com.specpulse.registry.ServiceEntity serviceRef = new com.specpulse.registry.ServiceEntity();
        serviceRef.setId(serviceId);
        serviceRef.setName(serviceName);
        entity.setService(serviceRef);
        entity.setVersionHash(newHash);

        // Parse JSON content to JsonNode for JSONB storage
        try {
            entity.setSpecContent(objectMapper.readTree(specContent));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON content: " + e.getMessage(), e);
        }

        entity.setSpecVersion(parseResult.specVersion());
        entity.setSpecTitle(parseResult.title());
        entity.setFileSizeBytes((long) specContent.length());
        entity.setPulledAt(Instant.now());

        SpecVersionEntity saved = repository.save(entity);
        log.info("Saved new version {} for service {} (hash: {})", saved.getId(), serviceName, newHash);

        return PullResult.newVersion(saved.getId(), newHash, latestVersion.map(SpecVersionEntity::getId));
    }

    @Transactional(readOnly = true)
    public SpecVersionDTO getVersionById(Long id) {
        SpecVersionEntity entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Spec version not found with id: " + id));
        return SpecVersionDTO.fromEntity(entity);
    }

    @Transactional(readOnly = true)
    public String getSpecContentById(Long id) {
        return repository.findById(id)
                .map(SpecVersionEntity::getSpecContent)
                .map(content -> {
                    // Convert JsonNode back to JSON string
                    try {
                        return objectMapper.writeValueAsString(content);
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize JSON content", e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("Spec version not found with id: " + id));
    }

    public record PullResult(
            boolean hasChanges,
            Long newVersionId,
            Long previousVersionId,
            String versionHash
    ) {
        public static PullResult unchanged(Long versionId, String hash) {
            return new PullResult(false, versionId, null, hash);
        }

        public static PullResult newVersion(Long newVersionId, String hash, Optional<Long> previousVersionId) {
            return new PullResult(true, newVersionId, previousVersionId.orElse(null), hash);
        }
    }
}
