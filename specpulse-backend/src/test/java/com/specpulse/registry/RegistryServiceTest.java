package com.specpulse.registry;

import com.specpulse.exception.DuplicateResourceException;
import com.specpulse.exception.ResourceNotFoundException;
import com.specpulse.group.ServiceGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistryServiceTest {

    @Mock
    private ServiceSearchRepository repository;

    @Mock
    private com.specpulse.version.VersionService versionService;

    @Mock
    private com.specpulse.client.OpenApiClient openApiClient;

    @Mock
    private ServiceGroupRepository groupRepository;

    private RegistryService registryService;

    @BeforeEach
    void setUp() {
        registryService = new RegistryService(repository, versionService, openApiClient, groupRepository);
    }

    @Test
    @DisplayName("Should create service successfully")
    void shouldCreateServiceSuccessfully() {
        // Given
        var request = new RegistryService.CreateServiceRequest(
                "test-service",
                "https://api.example.com/openapi.json",
                "Test service",
                true,
                null
        );

        var savedEntity = new ServiceEntity("test-service", "https://api.example.com/openapi.json", "Test service");
        savedEntity.setId(1L);
        savedEntity.setEnabled(true);

        given(repository.existsByName(request.name())).willReturn(false);
        given(repository.save(any(ServiceEntity.class))).willReturn(savedEntity);

        // When
        var result = registryService.createService(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("test-service");
        assertThat(result.enabled()).isTrue();
        verify(repository).save(any(ServiceEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when service name already exists")
    void shouldThrowExceptionWhenServiceNameExists() {
        // Given
        var request = new RegistryService.CreateServiceRequest(
                "existing-service",
                "https://api.example.com/openapi.json",
                "Test",
                true,
                null
        );

        given(repository.existsByName(request.name())).willReturn(true);

        // When & Then
        assertThatThrownBy(() -> registryService.createService(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should throw exception when URL format is invalid")
    void shouldThrowExceptionWhenUrlInvalid() {
        // Given
        var request = new RegistryService.CreateServiceRequest(
                "test-service",
                "invalid-url",
                "Test",
                true,
                null
        );

        given(repository.existsByName(request.name())).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> registryService.createService(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OpenAPI URL format");
    }

    @Test
    @DisplayName("Should update service successfully")
    void shouldUpdateServiceSuccessfully() {
        // Given
        Long serviceId = 1L;
        var request = new RegistryService.UpdateServiceRequest(
                "updated-name",
                "https://api.updated.com/openapi.json",
                "Updated description",
                false,
                null
        );

        var existingEntity = new ServiceEntity("old-name", "https://old.com/openapi.json", "Old desc");
        existingEntity.setId(serviceId);

        given(repository.findById(serviceId)).willReturn(Optional.of(existingEntity));
        given(repository.existsByName("updated-name")).willReturn(false);
        given(repository.save(any(ServiceEntity.class))).willReturn(existingEntity);

        // When
        var result = registryService.updateService(serviceId, request);

        // Then
        assertThat(result).isNotNull();
        verify(repository).findById(serviceId);
        verify(repository).save(any(ServiceEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent service")
    void shouldThrowExceptionWhenUpdatingNonExistent() {
        // Given
        Long serviceId = 999L;
        var request = new RegistryService.UpdateServiceRequest("name", "https://url.com", "desc", true, null);

        given(repository.findById(serviceId)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> registryService.updateService(serviceId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should delete service successfully")
    void shouldDeleteServiceSuccessfully() {
        // Given
        Long serviceId = 1L;
        given(repository.existsById(serviceId)).willReturn(true);

        // When
        registryService.deleteService(serviceId);

        // Then
        verify(repository).deleteById(serviceId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent service")
    void shouldThrowExceptionWhenDeletingNonExistent() {
        // Given
        Long serviceId = 999L;
        given(repository.existsById(serviceId)).willReturn(false);

        // When & Then
        assertThatThrownBy(() -> registryService.deleteService(serviceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should return all services")
    void shouldReturnAllServices() {
        // Given
        var entity1 = new ServiceEntity("service1", "https://api1.com/openapi.json", "Desc 1");
        entity1.setId(1L);
        var entity2 = new ServiceEntity("service2", "https://api2.com/openapi.json", "Desc 2");
        entity2.setId(2L);

        given(repository.findAll()).willReturn(List.of(entity1, entity2));

        // When
        var result = registryService.getAllServices();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("service1");
        assertThat(result.get(1).name()).isEqualTo("service2");
    }

    @Test
    @DisplayName("Should return enabled services only")
    void shouldReturnEnabledServicesOnly() {
        // Given
        var enabledEntity = new ServiceEntity("enabled", "https://enabled.com/openapi.json", "Enabled");
        enabledEntity.setId(1L);
        enabledEntity.setEnabled(true);

        var disabledEntity = new ServiceEntity("disabled", "https://disabled.com/openapi.json", "Disabled");
        disabledEntity.setId(2L);
        disabledEntity.setEnabled(false);

        given(repository.findByEnabledTrue()).willReturn(List.of(enabledEntity));

        // When
        var result = registryService.getEnabledServices();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("enabled");
    }

    @Test
    @DisplayName("Should return empty list for search suggestions when query is too short")
    void shouldReturnEmptyListForShortQuery() {
        // When
        var result = registryService.getSearchSuggestions("a");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return search suggestions")
    void shouldReturnSearchSuggestions() {
        // Given
        given(repository.suggestServiceNames("test")).willReturn(List.of("test-service", "test-api"));

        // When
        var result = registryService.getSearchSuggestions("test");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains("test-service", "test-api");
    }
}
