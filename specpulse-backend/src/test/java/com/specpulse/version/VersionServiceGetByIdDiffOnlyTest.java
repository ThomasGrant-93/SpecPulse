package com.specpulse.version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.registry.ServiceEntity;
import com.specpulse.parser.OpenApiParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class VersionServiceGetByIdDiffOnlyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Identical specs should return empty object ({}), not null")
    void identicalSpecsShouldReturnEmptyObject() {
        // Given
        SpecVersionRepository repository = Mockito.mock(SpecVersionRepository.class);
        OpenApiParser parser = Mockito.mock(OpenApiParser.class);
        VersionService service = new VersionService(repository, parser, objectMapper);

        ServiceEntity serviceMock = Mockito.mock(ServiceEntity.class);
        when(serviceMock.getId()).thenReturn(10L);

        JsonNode left = readTree("{\"a\":1}");
        JsonNode right = readTree("{\"a\":1}");

        SpecVersionEntity base = new SpecVersionEntity();
        base.setId(1L);
        base.setService(serviceMock);
        base.setVersionHash("hash");
        base.setSpecContent(left);
        base.setSpecVersion("3.0.0");
        base.setSpecTitle("title");
        base.setFileSizeBytes(0L);
        base.setPulledAt(Instant.parse("2020-01-01T00:00:00Z"));

        SpecVersionEntity other = new SpecVersionEntity();
        other.setId(2L);
        other.setService(serviceMock);
        other.setVersionHash("hash");
        other.setSpecContent(right);
        other.setSpecVersion("3.0.0");
        other.setSpecTitle("title");
        other.setFileSizeBytes(0L);
        other.setPulledAt(Instant.parse("2020-01-01T00:00:00Z"));

        when(repository.findById(1L)).thenReturn(Optional.of(base));
        when(repository.findById(2L)).thenReturn(Optional.of(other));

        // When
        SpecVersionDTO dto = service.getVersionByIdDiffOnly(1L, 2L);

        // Then
        assertThat(dto.specContent()).isEqualTo("{}");
    }

    @Test
    @DisplayName("Missing keys on the right should be emitted as null placeholders on the filtered left")
    void missingKeysShouldBecomeNullPlaceholders() {
        // Given
        SpecVersionRepository repository = Mockito.mock(SpecVersionRepository.class);
        OpenApiParser parser = Mockito.mock(OpenApiParser.class);
        VersionService service = new VersionService(repository, parser, objectMapper);

        ServiceEntity serviceMock = Mockito.mock(ServiceEntity.class);
        when(serviceMock.getId()).thenReturn(10L);

        JsonNode left = readTree("{\"a\":1}");
        JsonNode right = readTree("{\"a\":1,\"b\":2}");

        SpecVersionEntity base = new SpecVersionEntity();
        base.setId(1L);
        base.setService(serviceMock);
        base.setSpecContent(left);

        SpecVersionEntity other = new SpecVersionEntity();
        other.setId(2L);
        other.setService(serviceMock);
        other.setSpecContent(right);

        when(repository.findById(1L)).thenReturn(Optional.of(base));
        when(repository.findById(2L)).thenReturn(Optional.of(other));

        // When
        SpecVersionDTO dto = service.getVersionByIdDiffOnly(1L, 2L);

        // Then
        assertThat(dto.specContent()).isEqualTo("{\"b\":null}");
    }

    @Test
    @DisplayName("If arrays differ, the full left array should be emitted")
    void arraysDifferShouldEmitFullLeftArray() {
        // Given
        SpecVersionRepository repository = Mockito.mock(SpecVersionRepository.class);
        OpenApiParser parser = Mockito.mock(OpenApiParser.class);
        VersionService service = new VersionService(repository, parser, objectMapper);

        ServiceEntity serviceMock = Mockito.mock(ServiceEntity.class);
        when(serviceMock.getId()).thenReturn(10L);

        JsonNode left = readTree("{\"arr\":[1,2]}");
        JsonNode right = readTree("{\"arr\":[1,3]}");

        SpecVersionEntity base = new SpecVersionEntity();
        base.setId(1L);
        base.setService(serviceMock);
        base.setSpecContent(left);

        SpecVersionEntity other = new SpecVersionEntity();
        other.setId(2L);
        other.setService(serviceMock);
        other.setSpecContent(right);

        when(repository.findById(1L)).thenReturn(Optional.of(base));
        when(repository.findById(2L)).thenReturn(Optional.of(other));

        // When
        SpecVersionDTO dto = service.getVersionByIdDiffOnly(1L, 2L);

        // Then
        assertThat(dto.specContent()).isEqualTo("{\"arr\":[1,2]}");
    }

    @Test
    @DisplayName("Filtered output should have deterministic, sorted key order")
    void filteredOutputShouldBeDeterministicSorted() {
        // Given
        SpecVersionRepository repository = Mockito.mock(SpecVersionRepository.class);
        OpenApiParser parser = Mockito.mock(OpenApiParser.class);
        VersionService service = new VersionService(repository, parser, objectMapper);

        ServiceEntity serviceMock = Mockito.mock(ServiceEntity.class);
        when(serviceMock.getId()).thenReturn(10L);

        JsonNode left = readTree("{\"b\":1,\"a\":1,\"c\":1}");
        JsonNode right = readTree("{\"b\":2,\"a\":1,\"d\":1}");

        SpecVersionEntity base = new SpecVersionEntity();
        base.setId(1L);
        base.setService(serviceMock);
        base.setSpecContent(left);

        SpecVersionEntity other = new SpecVersionEntity();
        other.setId(2L);
        other.setService(serviceMock);
        other.setSpecContent(right);

        when(repository.findById(1L)).thenReturn(Optional.of(base));
        when(repository.findById(2L)).thenReturn(Optional.of(other));

        // When
        SpecVersionDTO dto1 = service.getVersionByIdDiffOnly(1L, 2L);
        SpecVersionDTO dto2 = service.getVersionByIdDiffOnly(1L, 2L);

        // Then
        assertThat(dto1.specContent()).isEqualTo(dto2.specContent());
        assertThat(dto1.specContent()).isEqualTo("{\"b\":1,\"c\":1,\"d\":null}");
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
