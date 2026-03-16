package com.specpulse.scheduler;

import com.specpulse.client.OpenApiClient;
import com.specpulse.client.SpecFetchResult;
import com.specpulse.diff.DiffService;
import com.specpulse.history.AuditService;
import com.specpulse.registry.RegistryService;
import com.specpulse.registry.ServiceDTO;
import com.specpulse.version.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled service for pulling OpenAPI specifications
 */
@Service
@ConditionalOnProperty(name = "specpulse.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SpecPullScheduler {

    private static final Logger log = LoggerFactory.getLogger(SpecPullScheduler.class);

    private final RegistryService registryService;
    private final OpenApiClient openApiClient;
    private final VersionService versionService;
    private final DiffService diffService;
    private final PullExecutionRepository executionRepository;
    private final AuditService auditService;

    public SpecPullScheduler(RegistryService registryService,
                             OpenApiClient openApiClient,
                             VersionService versionService,
                             DiffService diffService,
                             PullExecutionRepository executionRepository,
                             AuditService auditService) {
        this.registryService = registryService;
        this.openApiClient = openApiClient;
        this.versionService = versionService;
        this.diffService = diffService;
        this.executionRepository = executionRepository;
        this.auditService = auditService;
    }

    /**
     * Scheduled pull for all enabled services
     */
    @Scheduled(fixedRateString = "${specpulse.scheduler.pull-interval-seconds:300}000")
    public void pullAllEnabledServices() {
        log.info("Starting scheduled pull for all enabled services");
        List<ServiceDTO> services = registryService.getEnabledServices();

        int newVersionsCount = 0;
        int unchangedCount = 0;
        int failedCount = 0;

        for (ServiceDTO service : services) {
            PullResult result = pullServiceInternal(service);
            if (result.hasChanges()) {
                newVersionsCount++;
            } else if (result.success()) {
                unchangedCount++;
            } else {
                failedCount++;
            }
        }

        log.info("Completed scheduled pull: newVersions={}, unchanged={}, failed={}, total={}",
                newVersionsCount, unchangedCount, failedCount, services.size());
    }

    /**
     * Manual pull for all enabled services (called from controller)
     */
    public PullAllResult pullAllEnabledServicesManual() {
        log.info("Starting manual pull for all enabled services");
        List<ServiceDTO> services = registryService.getEnabledServices();

        int newVersionsCount = 0;
        int unchangedCount = 0;
        int failedCount = 0;

        for (ServiceDTO service : services) {
            PullResult result = pullServiceInternal(service);
            if (result.hasChanges()) {
                newVersionsCount++;
            } else if (result.success()) {
                unchangedCount++;
            } else {
                failedCount++;
            }
        }

        log.info("Completed manual pull: {} new versions, {} unchanged, {} failed",
                newVersionsCount, unchangedCount, failedCount);

        return new PullAllResult(newVersionsCount, unchangedCount, failedCount);
    }

    /**
     * Manual pull for a specific service (called from controller)
     */
    public PullResult pullServiceById(Long serviceId) {
        ServiceDTO service = registryService.getServiceById(serviceId);
        return pullServiceInternal(service);
    }

    /**
     * Internal pull logic for a single service
     */
    private PullResult pullServiceInternal(ServiceDTO service) {
        log.debug("Pulling OpenAPI spec for service: id={}, name={}, url={}",
                service.id(), service.name(), service.openApiUrl());
        Instant executionTime = Instant.now();

        PullExecutionEntity execution = new PullExecutionEntity(service.id(), "RUNNING", executionTime);

        try {
            // Fetch spec from URL
            SpecFetchResult result = openApiClient.fetchSpec(service.openApiUrl());
            execution.setHttpStatusCode(result.httpStatusCode());
            execution.setDurationMs(result.durationMs());

            if (!result.success()) {
                execution.setStatus("FAILED");
                execution.setErrorMessage(result.errorMessage());
                executionRepository.save(execution);

                auditService.logEvent(service.id(), AuditService.EventType.SPEC_FETCH_FAILED,
                        "Failed to fetch spec: " + result.errorMessage());

                log.warn("Failed to pull spec for service: id={}, name={}, error={}",
                        service.id(), service.name(), result.errorMessage());
                return PullResult.failed(result.errorMessage());
            }

            // Parse and save version (with hash comparison inside)
            VersionService.PullResult pullResult;
            try {
                pullResult = versionService.pullAndSaveVersion(
                        service.id(), service.name(), result.content());
            } catch (IllegalArgumentException e) {
                // Invalid OpenAPI spec
                execution.setStatus("FAILED");
                execution.setErrorMessage(e.getMessage());
                executionRepository.save(execution);

                auditService.logEvent(service.id(), AuditService.EventType.SPEC_FETCH_FAILED,
                        "Invalid OpenAPI spec: " + e.getMessage());

                log.error("Invalid OpenAPI spec for service: id={}, name={}, error={}",
                        service.id(), service.name(), e.getMessage());
                return PullResult.failed(e.getMessage());
            }

            execution.setStatus("SUCCESS");
            execution.setNewVersionCreated(pullResult.hasChanges());
            executionRepository.save(execution);

            if (pullResult.hasChanges()) {
                log.info("New version detected for service: id={}, name={}, versionId={}, hash={}",
                        service.id(), service.name(), pullResult.newVersionId(), pullResult.versionHash());

                auditService.logEvent(service.id(), pullResult.newVersionId(),
                        AuditService.EventType.SPEC_VERSION_CREATED,
                        "New version created with hash: " + pullResult.versionHash());

                // Compare with previous version if exists
                if (pullResult.previousVersionId() != null) {
                    try {
                        diffService.compareAndStore(
                                service.id(),
                                pullResult.previousVersionId(),
                                pullResult.newVersionId()
                        );

                        auditService.logEvent(service.id(), pullResult.newVersionId(),
                                AuditService.EventType.DIFF_ANALYZED,
                                "Diff analyzed between versions " + pullResult.previousVersionId() +
                                        " and " + pullResult.newVersionId());
                    } catch (RuntimeException e) {
                        log.error("Failed to compare versions for service: id={}, error={}",
                                service.id(), e.getMessage(), e);
                    }
                }
            } else {
                log.info("No changes for service: id={}, name={}, hash={}",
                        service.id(), service.name(), pullResult.versionHash());

                auditService.logEvent(service.id(), pullResult.newVersionId(),
                        AuditService.EventType.SPEC_VERSION_SKIPPED,
                        "Spec unchanged, hash: " + pullResult.versionHash());
            }

            return PullResult.success(pullResult.hasChanges(), pullResult.newVersionId(),
                    pullResult.previousVersionId(), pullResult.versionHash());

        } catch (RuntimeException e) {
            execution.setStatus("ERROR");
            execution.setErrorMessage(e.getMessage());
            executionRepository.save(execution);

            auditService.logEvent(service.id(), AuditService.EventType.SPEC_FETCH_FAILED,
                    "Error pulling spec: " + e.getMessage());

            log.error("Error pulling spec for service: id={}, name={}, error={}",
                    service.id(), service.name(), e.getMessage(), e);
            return PullResult.failed(e.getMessage());
        }
    }
}
