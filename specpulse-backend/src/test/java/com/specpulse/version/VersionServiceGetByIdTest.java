package com.specpulse.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.exception.ResourceNotFoundException;
import com.specpulse.parser.OpenApiParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionServiceGetByIdTest {

    @Mock
    private SpecVersionRepository repository;

    @Mock
    private OpenApiParser parser;

    @Test
    @DisplayName("Should throw ResourceNotFoundException when version is missing")
    void shouldThrowNotFoundWhenVersionMissing() {
        long id = 123L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        VersionService service = new VersionService(repository, parser, new ObjectMapper());

        assertThatThrownBy(() -> service.getVersionById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Spec version not found with id: " + id);
    }
}
