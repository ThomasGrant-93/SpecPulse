package com.specpulse.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void handleNoResourceFound_shouldReturn404() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        when(ex.getMessage()).thenReturn("No static resource favicon.ico");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleNoResource(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).contains("favicon.ico");
    }
}
