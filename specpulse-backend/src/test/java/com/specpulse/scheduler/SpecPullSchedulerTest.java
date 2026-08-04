package com.specpulse.scheduler;

import com.specpulse.client.OpenApiSpecPort;
import com.specpulse.client.SpecFetchResult;
import com.specpulse.diff.domain.SpecDiffPort;
import com.specpulse.history.AuditEventType;
import com.specpulse.history.domain.AuditLogPort;
import com.specpulse.registry.application.RegistryService;
import com.specpulse.registry.ServiceDTO;
import com.specpulse.scheduler.application.SpecPullScheduler;
import com.specpulse.scheduler.domain.PullExecutionEntity;
import com.specpulse.scheduler.domain.PullExecutionStorePort;
import com.specpulse.version.SpecVersionPullPort;
import com.specpulse.version.SpecVersionPullResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpecPullSchedulerTest {

    @Mock
    private RegistryService registryService;

    @Mock
    private OpenApiSpecPort openApiClient;

    @Mock
    private SpecVersionPullPort versionPullPort;

    @Mock
    private SpecDiffPort diffPort;

    @Mock
    private PullExecutionStorePort executionStore;

    @Mock
    private AuditLogPort auditLogPort;

    private SpecPullScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SpecPullScheduler(
                registryService,
                openApiClient,
                versionPullPort,
                diffPort,
                executionStore,
                auditLogPort
        );
    }

    @Test
    @DisplayName("Should return failed result when spec fetch fails")
    void shouldReturnFailedWhenSpecFetchFails() {
        ServiceDTO service = service(1L, "svc-1", "https://svc-1/openapi.yaml");
        given(registryService.getServiceById(1L)).willReturn(service);
        given(openApiClient.fetchSpec(service.openApiUrl()))
                .willReturn(SpecFetchResult.failure(500, "boom", 123L));

        PullResult result = scheduler.pullServiceById(1L);

        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo("boom");
        verify(versionPullPort, never()).pullAndSaveVersion(any(), any(), any());
        verify(auditLogPort).logEvent(1L, AuditEventType.SPEC_FETCH_FAILED, "Failed to fetch spec: boom");

        ArgumentCaptor<PullExecutionEntity> captor = ArgumentCaptor.forClass(PullExecutionEntity.class);
        verify(executionStore).save(captor.capture());
        PullExecutionEntity execution = captor.getValue();
        assertThat(execution.getStatus()).isEqualTo("FAILED");
        assertThat(execution.getHttpStatusCode()).isEqualTo(500);
        assertThat(execution.getErrorMessage()).isEqualTo("boom");
    }

    @Test
    @DisplayName("Should analyze diff when new version has previous version")
    void shouldAnalyzeDiffWhenPreviousVersionExists() {
        ServiceDTO service = service(2L, "svc-2", "https://svc-2/openapi.json");
        given(registryService.getServiceById(2L)).willReturn(service);
        given(openApiClient.fetchSpec(service.openApiUrl()))
                .willReturn(SpecFetchResult.success("{\"openapi\":\"3.0.0\"}", 200, 45L));
        given(versionPullPort.pullAndSaveVersion(2L, "svc-2", "{\"openapi\":\"3.0.0\"}"))
                .willReturn(new SpecVersionPullResult(true, 20L, 10L, "hash-20"));

        PullResult result = scheduler.pullServiceById(2L);

        assertThat(result.success()).isTrue();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.newVersionId()).isEqualTo(20L);
        assertThat(result.previousVersionId()).isEqualTo(10L);
        verify(diffPort).analyzeAndStore(2L, 10L, 20L);
        verify(auditLogPort).logEvent(2L, 20L, AuditEventType.SPEC_VERSION_CREATED,
                "New version created with hash: hash-20");
        verify(auditLogPort).logEvent(2L, 20L, AuditEventType.DIFF_ANALYZED,
                "Diff analyzed between versions 10 and 20");

        ArgumentCaptor<PullExecutionEntity> captor = ArgumentCaptor.forClass(PullExecutionEntity.class);
        verify(executionStore).save(captor.capture());
        PullExecutionEntity execution = captor.getValue();
        assertThat(execution.getStatus()).isEqualTo("SUCCESS");
        assertThat(execution.getNewVersionCreated()).isTrue();
    }

    @Test
    @DisplayName("Should return manual pull aggregate counts")
    void shouldReturnManualPullAggregateCounts() {
        ServiceDTO service1 = service(1L, "svc-1", "https://svc-1/openapi.json");
        ServiceDTO service2 = service(2L, "svc-2", "https://svc-2/openapi.json");
        ServiceDTO service3 = service(3L, "svc-3", "https://svc-3/openapi.json");

        given(registryService.getEnabledServices()).willReturn(List.of(service1, service2, service3));
        given(openApiClient.fetchSpec(service1.openApiUrl()))
                .willReturn(SpecFetchResult.success("v1", 200, 10L));
        given(openApiClient.fetchSpec(service2.openApiUrl()))
                .willReturn(SpecFetchResult.success("v2", 200, 12L));
        given(openApiClient.fetchSpec(service3.openApiUrl()))
                .willReturn(SpecFetchResult.failure(404, "not found", 8L));

        given(versionPullPort.pullAndSaveVersion(1L, "svc-1", "v1"))
                .willReturn(new SpecVersionPullResult(true, 100L, 90L, "h1"));
        given(versionPullPort.pullAndSaveVersion(2L, "svc-2", "v2"))
                .willReturn(new SpecVersionPullResult(false, 200L, null, "h2"));

        PullAllResult result = scheduler.pullAllEnabledServicesManual();

        assertThat(result.newVersions()).isEqualTo(1);
        assertThat(result.unchanged()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
    }

    private ServiceDTO service(Long id, String name, String url) {
        return new ServiceDTO(id, name, url, "", true, null, null);
    }
}
