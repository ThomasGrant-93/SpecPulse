package com.specpulse.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiParserTest {

    private final OpenApiParser parser = new OpenApiParser();

    @Test
    @DisplayName("Should parse valid OpenAPI 3.0 spec")
    void shouldParseValidOpenApi3Spec() {
        // Given
        String spec = """
                {
                    "openapi": "3.0.0",
                    "info": {
                        "title": "Test API",
                        "version": "1.0.0",
                        "description": "Test Description"
                    },
                    "paths": {
                        "/api/test": {
                            "get": {
                                "summary": "Test endpoint",
                                "responses": {
                                    "200": {
                                        "description": "Success"
                                    }
                                }
                            }
                        }
                    }
                }
                """;

        // When
        OpenApiParser.ParseResult result = parser.parse(spec);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.title()).isEqualTo("Test API");
        assertThat(result.specVersion()).isEqualTo("3.0.0");
        assertThat(result.contentHash()).isNotNull();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    @DisplayName("Should parse valid OpenAPI 3.1 spec")
    void shouldParseValidOpenApi31Spec() {
        // Given
        String spec = """
                {
                    "openapi": "3.1.0",
                    "info": {
                        "title": "Test API",
                        "version": "2.0.0"
                    },
                    "paths": {}
                }
                """;

        // When
        OpenApiParser.ParseResult result = parser.parse(spec);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.specVersion()).isEqualTo("3.1.0");
    }

    @Test
    @DisplayName("Should reject Swagger 2.0 spec")
    void shouldRejectSwagger2Spec() {
        // Given
        String spec = """
                {
                    "swagger": "2.0",
                    "info": {
                        "title": "Legacy API",
                        "version": "1.0.0"
                    },
                    "paths": {}
                }
                """;

        // When
        OpenApiParser.ParseResult result = parser.parse(spec);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Swagger 2.0");
        assertThat(result.errorMessage()).contains("not supported");
    }

    @Test
    @DisplayName("Should fail for invalid JSON")
    void shouldFailForInvalidJson() {
        // Given
        String spec = "{ invalid json }";

        // When
        OpenApiParser.ParseResult result = parser.parse(spec);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
    }

    @Test
    @DisplayName("Should fail for missing required fields")
    void shouldFailForMissingRequiredFields() {
        // Given
        String spec = """
                {
                    "openapi": "3.0.0"
                }
                """;

        // When
        OpenApiParser.ParseResult result = parser.parse(spec);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isNotNull();
    }

    @Test
    @DisplayName("Should generate consistent hash for same content")
    void shouldGenerateConsistentHash() {
        // Given
        String spec = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "API", "version": "1.0.0"},
                    "paths": {}
                }
                """;

        // When
        OpenApiParser.ParseResult result1 = parser.parse(spec);
        OpenApiParser.ParseResult result2 = parser.parse(spec);

        // Then
        assertThat(result1.contentHash()).isEqualTo(result2.contentHash());
    }

    @Test
    @DisplayName("Should generate different hashes for different content")
    void shouldGenerateDifferentHashes() {
        // Given
        String spec1 = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "API v1", "version": "1.0.0"},
                    "paths": {}
                }
                """;

        String spec2 = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "API v2", "version": "2.0.0"},
                    "paths": {}
                }
                """;

        // When
        OpenApiParser.ParseResult result1 = parser.parse(spec1);
        OpenApiParser.ParseResult result2 = parser.parse(spec2);

        // Then
        assertThat(result1.contentHash()).isNotEqualTo(result2.contentHash());
    }

    @Test
    @DisplayName("Should handle spec with complex paths")
    void shouldHandleComplexPaths() {
        // Given
        String spec = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "Complex API", "version": "1.0.0"},
                    "paths": {
                        "/api/v1/users": {
                            "get": {
                                "summary": "Get users",
                                "operationId": "getUsers",
                                "parameters": [
                                    {
                                        "name": "limit",
                                        "in": "query",
                                        "schema": {"type": "integer"}
                                    }
                                ],
                                "responses": {
                                    "200": {
                                        "description": "Success",
                                        "content": {
                                            "application/json": {
                                                "schema": {
                                                    "type": "array",
                                                    "items": {"type": "object"}
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            "post": {
                                "summary": "Create user",
                                "requestBody": {
                                    "content": {
                                        "application/json": {
                                            "schema": {"type": "object"}
                                        }
                                    }
                                },
                                "responses": {
                                    "201": {"description": "Created"}
                                }
                            }
                        }
                    }
                }
                """;

        // When
        OpenApiParser.ParseResult result = parser.parse(spec);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.title()).isEqualTo("Complex API");
    }

    @Test
    @DisplayName("Should handle empty paths object")
    void shouldHandleEmptyPaths() {
        // Given
        String spec = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "Empty API", "version": "1.0.0"},
                    "paths": {}
                }
                """;

        // When
        OpenApiParser.ParseResult result = parser.parse(spec);

        // Then
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("Should compute hash for content")
    void shouldComputeHashForContent() {
        // Given
        String spec = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "Test", "version": "1.0.0"},
                    "paths": {}
                }
                """;

        // When
        OpenApiParser.ParseResult result = parser.parse(spec);

        // Then
        assertThat(result.contentHash()).hasSize(64); // SHA-256 hex length
        assertThat(result.contentHash()).matches("[a-f0-9]+");
    }
}
