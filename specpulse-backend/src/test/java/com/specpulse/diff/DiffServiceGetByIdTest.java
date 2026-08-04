package com.specpulse.diff;

import com.specpulse.exception.ResourceNotFoundException;
import com.specpulse.diff.application.DiffService;
import com.specpulse.version.application.VersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiffServiceGetByIdTest {

    @Mock
    private com.specpulse.diff.infrastructure.SpecDiffRepository repository;

    @Mock
    private VersionService versionService;

    @Test
    @DisplayName("Should throw ResourceNotFoundException when diff is missing")
    void shouldThrowNotFoundWhenDiffMissing() {
        long id = 123L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        DiffService service = new DiffService(repository, versionService);

        assertThatThrownBy(() -> service.getDiffById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Diff not found with id: " + id);
    }
}
