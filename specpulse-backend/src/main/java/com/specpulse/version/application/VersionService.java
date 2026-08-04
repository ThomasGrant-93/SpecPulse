package com.specpulse.version.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.specpulse.exception.ResourceNotFoundException;
import com.specpulse.parser.OpenApiParser;
import com.specpulse.version.SpecVersionDTO;
import com.specpulse.version.SpecVersionPullPort;
import com.specpulse.version.SpecVersionPullResult;
import com.specpulse.version.infrastructure.SpecVersionRepository;
import com.specpulse.version.domain.SpecVersionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class VersionService implements SpecVersionPullPort {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    // Guardrails to prevent extremely expensive filtering on very large specs.
    // When exceeded, we fall back to emitting the full left subtree to keep the API responsive.
    private static final long FILTER_TIME_BUDGET_NANOS = 750_000_000L; // 750ms
    private static final int MAX_FILTER_DEPTH = 200;
    private final SpecVersionRepository repository;
    private final OpenApiParser parser;
    private final ObjectMapper objectMapper;
    @Value("${specpulse.outbound.max-spec-bytes:2097152}")
    private long maxSpecBytes = 2_097_152L;

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

    /**
     * Batch version fetch (latest per service) to avoid N+1 lookups.
     *
     * <p>Returns entities to avoid serializing potentially large OpenAPI content.
     */
    @Transactional(readOnly = true)
    public List<SpecVersionEntity> getLatestVersionEntitiesByServiceIds(Set<Long> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return List.of();
        }
        return repository.findLatestByServiceIds(serviceIds);
    }

    /**
     * Pull OpenAPI spec from service and save if changed.
     *
     * @return PullResult with information about changes.
     */
    @Transactional
    @Override
    public SpecVersionPullResult pullAndSaveVersion(Long serviceId, String serviceName, String specContent) {
        if (specContent == null || specContent.isBlank()) {
            throw new IllegalArgumentException("OpenAPI content is empty");
        }

        // Prevent CPU/memory DoS from very large specs.
        if (specContent.length() > maxSpecBytes) {
            throw new IllegalArgumentException("OpenAPI spec is too large");
        }

        OpenApiParser.ParseResult parseResult = parser.parse(specContent);

        if (!parseResult.success()) {
            throw new IllegalArgumentException("Failed to parse OpenAPI spec: " + parseResult.errorMessage());
        }

        String newHash = parseResult.contentHash();
        String contentToStore = parseResult.content();

        // Get latest version to compare hash
        Optional<SpecVersionEntity> latestVersion = repository.findFirstByServiceIdOrderByPulledAtDesc(serviceId);

        // Check if version with same hash already exists
        if (latestVersion.isPresent() && latestVersion.get().getVersionHash().equals(newHash)) {
            log.info("No changes detected for service {} (hash: {})", serviceName, newHash);
            return SpecVersionPullResult.unchanged(latestVersion.get().getId(), newHash);
        }

        // Create a minimal service entity reference for saving
        SpecVersionEntity entity = new SpecVersionEntity();
        com.specpulse.registry.domain.ServiceEntity serviceRef = new com.specpulse.registry.domain.ServiceEntity();
        serviceRef.setId(serviceId);
        serviceRef.setName(serviceName);
        entity.setService(serviceRef);
        entity.setVersionHash(newHash);

        // Parse content to JsonNode for JSONB storage (JSON or YAML)
        try {
            entity.setSpecContent(parseSpecContent(contentToStore));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid OpenAPI content format. Expected valid JSON or YAML.", e);
        }

        entity.setSpecVersion(parseResult.specVersion());
        entity.setSpecTitle(parseResult.title());
        entity.setFileSizeBytes((long) contentToStore.length());
        entity.setPulledAt(Instant.now());

        SpecVersionEntity saved = repository.save(entity);
        log.info("Saved new version {} for service {} (hash: {})", saved.getId(), serviceName, newHash);

        return SpecVersionPullResult.newVersion(
                saved.getId(),
                newHash,
                latestVersion.map(SpecVersionEntity::getId).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public SpecVersionDTO getVersionById(Long id) {
        SpecVersionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spec version", id));
        return SpecVersionDTO.fromEntity(entity);
    }

    @Transactional(readOnly = true)
    public SpecVersionDTO getVersionByIdDiffOnly(Long id, Long compareToId) {
        SpecVersionEntity base = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spec version", id));
        SpecVersionEntity other = repository.findById(compareToId)
                .orElseThrow(() -> new ResourceNotFoundException("Spec version", compareToId));

        JsonNode baseNode = base.getSpecContent();
        JsonNode otherNode = other.getSpecContent();

        boolean[] timedOut = {false};
        long deadlineNanos = System.nanoTime() + FILTER_TIME_BUDGET_NANOS;
        JsonNode filtered = filterDiff(baseNode, otherNode, deadlineNanos, 0, timedOut);
        if (filtered == null) {
            filtered = objectMapper.createObjectNode();
        }

        if (timedOut[0]) {
            log.debug("Spec diff filtering hit budget (serviceVersionId={}, compareToId={}) - falling back to emitting left subtree", id, compareToId);
        }

        String contentString;
        try {
            contentString = objectMapper.writeValueAsString(filtered);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize filtered OpenAPI content", e);
        }

        return new SpecVersionDTO(
                base.getId(),
                base.getService().getId(),
                base.getVersionHash(),
                base.getSpecVersion(),
                base.getSpecTitle(),
                base.getFileSizeBytes(),
                base.getPulledAt(),
                contentString
        );
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

    private JsonNode parseSpecContent(String specContent) throws com.fasterxml.jackson.core.JsonProcessingException {
        try {
            return objectMapper.readTree(specContent);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            return YAML_MAPPER.readTree(specContent);
        }
    }

    /**
     * Filter JSON to include only parts that differ between left and right.
     * <p>
     * Rules (left-side view):
     * - If a node is equal => omitted from output.
     * - If an object key exists only on the right => emitted as null on the left side.
     * - If an array differs => the full left array is emitted.
     */
    private JsonNode filterDiff(JsonNode left, JsonNode right, long deadlineNanos, int depth, boolean[] timedOut) {
        if (System.nanoTime() > deadlineNanos || depth > MAX_FILTER_DEPTH) {
            timedOut[0] = true;
            return fallbackForTimeout(left, right);
        }

        if (left == null && right == null) {
            return null;
        }
        if (left == null) {
            return NullNode.getInstance();
        }
        if (right == null) {
            return left;
        }
        if (left.equals(right)) {
            return null;
        }

        if (left.isObject() && right.isObject()) {
            ObjectNode out = objectMapper.createObjectNode();
            // Union of keys (sorted for deterministic output)
            java.util.Set<String> keys = new java.util.TreeSet<>();
            left.fieldNames().forEachRemaining(keys::add);
            right.fieldNames().forEachRemaining(keys::add);

            for (String key : keys) {
                JsonNode l = left.get(key);
                JsonNode r = right.get(key);
                JsonNode filteredChild = filterDiff(l, r, deadlineNanos, depth + 1, timedOut);
                if (filteredChild != null) {
                    out.set(key, filteredChild);
                }
            }

            return out.size() == 0 ? null : out;
        }

        if (left.isArray() && right.isArray()) {
            // If arrays differ, emit full left array to preserve readability.
            return left;
        }

        // Primitive/value mismatch.
        return left;
    }

    private JsonNode fallbackForTimeout(JsonNode left, JsonNode right) {
        // Preserve the original null placeholder semantics when possible.
        if (left == null && right != null) {
            return NullNode.getInstance();
        }
        return left;
    }
}
