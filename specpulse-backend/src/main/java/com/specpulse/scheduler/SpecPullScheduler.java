package com.specpulse.scheduler;

import com.specpulse.client.OpenApiSpecPort;
import com.specpulse.client.SpecFetchResult;
import com.specpulse.diff.SpecDiffPort;
import com.specpulse.history.AuditEventType;
import com.specpulse.history.AuditLogPort;
import com.specpulse.registry.RegistryService;
import com.specpulse.registry.ServiceDTO;
import com.specpulse.settings.ApplicationSettingService;
import com.specpulse.version.SpecVersionPullPort;
import com.specpulse.version.SpecVersionPullResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * Scheduled service for pulling OpenAPI specifications
 */
@Service
public class SpecPullScheduler {

    private static final Logger log = LoggerFactory.getLogger(SpecPullScheduler.class);

    private final RegistryService registryService;
    private final OpenApiSpecPort openApiClient;
    private final SpecVersionPullPort versionPullPort;
    private final SpecDiffPort diffPort;
    private final PullExecutionStorePort executionStore;
    private final AuditLogPort auditLogPort;
    private final Object scheduleLock = new Object();
    @Autowired(required = false)
    private TaskScheduler taskScheduler;
    @Autowired(required = false)
    private ApplicationSettingService settingService;
    @Value("${specpulse.scheduler.pull-interval-seconds:300}")
    private long defaultPullIntervalSeconds;
    @Value("${specpulse.scheduler.disabled-check-interval-seconds:60}")
    private long disabledCheckIntervalSeconds;
    private volatile boolean schedulingStarted = false;

    public SpecPullScheduler(RegistryService registryService,
                             OpenApiSpecPort openApiClient,
                             SpecVersionPullPort versionPullPort,
                             SpecDiffPort diffPort,
                             PullExecutionStorePort executionStore,
                             AuditLogPort auditLogPort) {
        this.registryService = registryService;
        this.openApiClient = openApiClient;
        this.versionPullPort = versionPullPort;
        this.diffPort = diffPort;
        this.executionStore = executionStore;
        this.auditLogPort = auditLogPort;
    }

    @PostConstruct
    public void initScheduler() {
        // Unit tests instantiate this class directly (not Spring-managed), so these may be null.
        if (taskScheduler == null || settingService == null) {
            return;
        }

        synchronized (scheduleLock) {
            if (schedulingStarted) {
                return;
            }
            schedulingStarted = true;
        }

        scheduleNextRun();
    }

    /**
     * Executes scheduled pull for all enabled services.
     */
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

    private void scheduleNextRun() {
        boolean schedulerEnabled = isSchedulerPullEnabled();

        if (!schedulerEnabled) {
            Date next = new Date(System.currentTimeMillis() + disabledCheckIntervalSeconds * 1000L);
            taskScheduler.schedule(this::runScheduledPullAndReschedule, next);
            return;
        }

        String cronExpression = getSchedulerCronExpression();
        if (cronExpression != null && !cronExpression.isBlank()) {
            try {
                CronExpression cron = CronExpression.parse(cronExpression);
                ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
                ZonedDateTime nextTime = cron.next(now);
                if (nextTime != null) {
                    Date next = Date.from(nextTime.toInstant());
                    taskScheduler.schedule(this::runScheduledPullAndReschedule, next);
                    return;
                }
            } catch (Exception e) {
                log.warn("Invalid cron expression '{}'; falling back to interval scheduling. error={}",
                        cronExpression, e.getMessage());
            }
        }

        long intervalSeconds = getSchedulerPullIntervalSecondsFromDb();
        if (intervalSeconds <= 0) {
            intervalSeconds = defaultPullIntervalSeconds;
        }

        long delayMs = intervalSeconds * 1000L;
        Date next = new Date(System.currentTimeMillis() + delayMs);
        taskScheduler.schedule(this::runScheduledPullAndReschedule, next);
    }

    private void runScheduledPullAndReschedule() {
        try {
            if (isSchedulerPullEnabled()) {
                pullAllEnabledServices();
            }
        } finally {
            scheduleNextRun();
        }
    }

    private boolean isSchedulerPullEnabled() {
        try {
            // DB keys from V6__Create_application_settings.sql
            // category: scheduler, key: pull.enabled
            return Boolean.TRUE.equals(settingService.getSettingValue("scheduler", "pull.enabled", Boolean.class));
        } catch (Exception e) {
            return true;
        }
    }

    private String getSchedulerCronExpression() {
        try {
            // category: scheduler, key: pull.cron_expression
            return settingService.getSettingValue("scheduler", "pull.cron_expression", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private long getSchedulerPullIntervalSecondsFromDb() {
        // Backward/alternate key names.
        try {
            Integer v = settingService.getSettingValue("scheduler", "pull_interval_seconds", Integer.class);
            if (v != null) {
                return v.longValue();
            }
        } catch (Exception ignored) {
            // ignore
        }

        try {
            Integer v = settingService.getSettingValue("scheduler", "pull_interval", Integer.class);
            if (v != null) {
                return v.longValue();
            }
        } catch (Exception ignored) {
            // ignore
        }

        return -1;
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
                executionStore.save(execution);

                auditLogPort.logEvent(service.id(), AuditEventType.SPEC_FETCH_FAILED,
                        "Failed to fetch spec: " + result.errorMessage());

                log.warn("Failed to pull spec for service: id={}, name={}, error={}",
                        service.id(), service.name(), result.errorMessage());
                return PullResult.failed(result.errorMessage());
            }

            // Parse and save version (with hash comparison inside)
            SpecVersionPullResult pullResult;
            try {
                pullResult = versionPullPort.pullAndSaveVersion(
                        service.id(), service.name(), result.content());
            } catch (IllegalArgumentException e) {
                // Invalid OpenAPI spec
                execution.setStatus("FAILED");
                execution.setErrorMessage(e.getMessage());
                executionStore.save(execution);

                auditLogPort.logEvent(service.id(), AuditEventType.SPEC_FETCH_FAILED,
                        "Invalid OpenAPI spec: " + e.getMessage());

                log.error("Invalid OpenAPI spec for service: id={}, name={}, error={}",
                        service.id(), service.name(), e.getMessage());
                return PullResult.failed(e.getMessage());
            }

            execution.setStatus("SUCCESS");
            execution.setNewVersionCreated(pullResult.hasChanges());
            executionStore.save(execution);

            if (pullResult.hasChanges()) {
                log.info("New version detected for service: id={}, name={}, versionId={}, hash={}",
                        service.id(), service.name(), pullResult.newVersionId(), pullResult.versionHash());

                auditLogPort.logEvent(service.id(), pullResult.newVersionId(),
                        AuditEventType.SPEC_VERSION_CREATED,
                        "New version created with hash: " + pullResult.versionHash());

                // Compare with previous version if exists
                if (pullResult.previousVersionId() != null) {
                    try {
                        diffPort.analyzeAndStore(
                                service.id(),
                                pullResult.previousVersionId(),
                                pullResult.newVersionId()
                        );

                        auditLogPort.logEvent(service.id(), pullResult.newVersionId(),
                                AuditEventType.DIFF_ANALYZED,
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

                auditLogPort.logEvent(service.id(), pullResult.newVersionId(),
                        AuditEventType.SPEC_VERSION_SKIPPED,
                        "Spec unchanged, hash: " + pullResult.versionHash());
            }

            return PullResult.success(pullResult.hasChanges(), pullResult.newVersionId(),
                    pullResult.previousVersionId(), pullResult.versionHash());

        } catch (RuntimeException e) {
            execution.setStatus("ERROR");
            execution.setErrorMessage(e.getMessage());
            executionStore.save(execution);

            auditLogPort.logEvent(service.id(), AuditEventType.SPEC_FETCH_FAILED,
                    "Error pulling spec: " + e.getMessage());

            log.error("Error pulling spec for service: id={}, name={}, error={}",
                    service.id(), service.name(), e.getMessage(), e);
            return PullResult.failed(e.getMessage());
        }
    }
}
