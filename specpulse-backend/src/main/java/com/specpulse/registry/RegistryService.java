package com.specpulse.registry;

import com.specpulse.client.OpenApiSpecPort;
import com.specpulse.client.OutboundUrlSecurity;
import com.specpulse.exception.DuplicateResourceException;
import com.specpulse.exception.ResourceNotFoundException;
import com.specpulse.version.VersionService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class RegistryService {

    private final RegistryRepositoryPort repository;
    private final VersionService versionService;
    private final OpenApiSpecPort openApiClient;
    private final ServiceGroupLookupPort groupLookupPort;
    private final OutboundUrlSecurity outboundUrlSecurity;

    @Transactional(readOnly = true)
    public List<ServiceDTO> getAllServices() {
        return repository.findAll().stream()
                .map(ServiceDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceWithVersionDTO> getAllServicesWithVersions() {
        return repository.findAll().stream()
                .map(entity -> {
                    var latestVersion = versionService.getLatestVersion(entity.getId())
                            .map(v -> new ServiceWithVersionDTO.LatestVersionInfo(
                                    v.id(), v.versionHash(), v.specVersion(), v.specTitle(), v.fileSizeBytes(), v.pulledAt()
                            ))
                            .orElse(null);
                    return ServiceWithVersionDTO.fromEntity(entity, latestVersion);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceDTO> getEnabledServices() {
        return repository.findByEnabledTrue().stream()
                .map(ServiceDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ServiceDTO getServiceById(Long id) {
        ServiceEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
        return ServiceDTO.fromEntity(entity);
    }

    @Transactional
    public ServiceDTO createService(CreateServiceRequest request) {
        log.info("Creating new service: name={}, url={}, groupId={}", request.name(), request.openApiUrl(), request.groupId());

        if (repository.existsByName(request.name())) {
            log.warn("Service creation failed - name already exists: {}", request.name());
            throw new DuplicateResourceException("Service with name '" + request.name() + "' already exists");
        }

        // Validate OpenAPI URL (SSRF hardening happens inside outboundUrlSecurity)
        outboundUrlSecurity.validateOpenApiUrl(request.openApiUrl());

        ServiceEntity entity = new ServiceEntity(
                request.name(),
                request.openApiUrl(),
                request.description()
        );
        entity.setEnabled(request.enabled() != null ? request.enabled() : true);

        // Set group if provided
        if (request.groupId() != null) {
            groupLookupPort.findById(request.groupId())
                    .ifPresentOrElse(
                            entity::setGroup,
                            () -> log.warn("Group with id {} not found, service will be created without group", request.groupId())
                    );
        }

        ServiceEntity saved = repository.save(entity);

        log.info("Service created successfully: id={}, name={}, enabled={}, groupId={}",
                saved.getId(), saved.getName(), saved.isEnabled(), saved.getGroup() != null ? saved.getGroup().getId() : null);
        return ServiceDTO.fromEntity(saved);
    }

    @Transactional
    public ServiceDTO updateService(Long id, UpdateServiceRequest request) {
        log.info("Updating service: id={}, request={}", id, request);

        ServiceEntity entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Service update failed - not found: id={}", id);
                    return new ResourceNotFoundException("Service not found with id: " + id);
                });

        if (request.name() != null) {
            if (!entity.getName().equals(request.name()) && repository.existsByName(request.name())) {
                log.warn("Service update failed - name already exists: {}", request.name());
                throw new DuplicateResourceException("Service with name '" + request.name() + "' already exists");
            }
            entity.setName(request.name());
        }
        if (request.openApiUrl() != null) {
            outboundUrlSecurity.validateOpenApiUrl(request.openApiUrl());
            entity.setOpenApiUrl(request.openApiUrl());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.enabled() != null) {
            entity.setEnabled(request.enabled());
        }
        if (request.groupId() != null) {
            groupLookupPort.findById(request.groupId())
                    .ifPresentOrElse(
                            entity::setGroup,
                            () -> log.warn("Group with id {} not found, group assignment skipped", request.groupId())
                    );
        } else if (Boolean.TRUE.equals(request.clearGroup())) {
            entity.setGroup(null);
        }

        ServiceEntity saved = repository.save(entity);
        log.info("Service updated successfully: id={}, name={}, groupId={}", saved.getId(), saved.getName(), saved.getGroup() != null ? saved.getGroup().getId() : null);
        return ServiceDTO.fromEntity(saved);
    }

    @Transactional
    public void deleteService(Long id) {
        log.info("Deleting service: id={}", id);

        if (!repository.existsById(id)) {
            log.warn("Service deletion failed - not found: id={}", id);
            throw new ResourceNotFoundException("Service not found with id: " + id);
        }

        repository.deleteById(id);
        log.info("Service deleted successfully: id={}", id);
    }

    @Transactional(readOnly = true)
    public List<ServiceDTO> searchServices(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllServices();
        }
        return repository.search(query.trim()).stream()
                .map(ServiceDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        String trimmedQuery = query.trim().toLowerCase();
        List<String> suggestions = repository.suggestServiceNames(trimmedQuery);

        // Add description-based suggestions if we don't have enough
        if (suggestions.size() < 5) {
            List<String> descSuggestions = repository.suggestByDescription(trimmedQuery);
            for (String suggestion : descSuggestions) {
                if (!suggestions.contains(suggestion) && suggestions.size() < 10) {
                    suggestions.add(suggestion);
                }
            }
        }

        return suggestions;
    }

    /**
     * Validate service before creation
     */
    public ServiceValidationResult validateService(ServiceValidationRequest request) {
        List<String> errors = new java.util.ArrayList<>();

        // Check if name already exists
        if (repository.existsByName(request.name())) {
            errors.add("Service with name '" + request.name() + "' already exists");
        }

        // Validate OpenAPI URL
        var validationResult = openApiClient.validateOpenApiSpec(request.openApiUrl());
        if (!validationResult.success()) {
            errors.addAll(validationResult.errors());
        }

        return new ServiceValidationResult(
                errors.isEmpty(),
                errors
        );
    }

    @Schema(
            description = "Create a new service registration",
            example = "{\"name\":\"petstore\",\"openApiUrl\":\"https://api.example.com/openapi.json\",\"description\":\"Example service\",\"enabled\":true,\"groupId\":1}"
    )
    public record CreateServiceRequest(
            @Schema(description = "Service display name", example = "petstore")
            @NotBlank(message = "Name is required")
            String name,
            @Schema(description = "OpenAPI document URL", example = "https://api.example.com/openapi.json")
            @NotBlank(message = "OpenAPI URL is required")
            @Pattern(regexp = "^https?://.+$", message = "Invalid URL format. Must start with http:// or https://")
            String openApiUrl,
            @Schema(description = "Optional service description", example = "Example service")
            String description,
            @Schema(description = "Whether the service is enabled", example = "true")
            Boolean enabled,
            @Schema(description = "Optional service group id", example = "1")
            Long groupId
    ) {
    }

    @Schema(
            description = "Update an existing service registration",
            example = "{\"name\":\"petstore\",\"openApiUrl\":\"https://api.example.com/openapi.json\",\"description\":\"Example service\",\"enabled\":true,\"groupId\":1,\"clearGroup\":false}"
    )
    public record UpdateServiceRequest(
            @Schema(description = "Optional service display name", example = "petstore")
            String name,
            @Schema(description = "Optional OpenAPI document URL", example = "https://api.example.com/openapi.json")
            @Pattern(regexp = "^https?://.+$", message = "Invalid URL format. Must start with http:// or https://")
            String openApiUrl,
            @Schema(description = "Optional service description", example = "Example service")
            String description,
            @Schema(description = "Optional enabled flag", example = "true")
            Boolean enabled,
            @Schema(description = "Optional service group id", example = "1")
            Long groupId,
            @Schema(description = "When true, clears existing group assignment", example = "false")
            Boolean clearGroup
    ) {
    }
}
