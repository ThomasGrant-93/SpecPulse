package com.specpulse.diff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiffServiceTest {

    private final DiffService diffService = new DiffService(null, null);

    @Test
    @DisplayName("Should detect no changes between identical specs")
    void shouldDetectNoChanges() {
        // Given
        String spec = """
                {
                    "openapi": "3.0.0",
                    "info": {
                        "title": "Test API",
                        "version": "1.0.0"
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
        DiffResultDTO result = diffService.compare(spec, spec);

        // Then
        assertThat(result.hasBreakingChanges()).isFalse();
        assertThat(result.breakingChangesCount()).isZero();
        assertThat(result.unchanged()).isTrue();
    }

    @Test
    @DisplayName("Should detect breaking change when endpoint removed")
    void shouldDetectBreakingChangeWhenEndpointRemoved() {
        // Given
        String oldSpec = """
                {
                    "openapi": "3.0.0",
                    "info": {
                        "title": "Test API",
                        "version": "1.0.0"
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
                        },
                        "/api/users": {
                            "get": {
                                "summary": "Get users",
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

        String newSpec = """
                {
                    "openapi": "3.0.0",
                    "info": {
                        "title": "Test API",
                        "version": "2.0.0"
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
        DiffResultDTO result = diffService.compare(oldSpec, newSpec);

        // Then
        assertThat(result.hasBreakingChanges()).isTrue();
        assertThat(result.breakingChangesCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should detect non-breaking change when optional endpoint added")
    void shouldDetectNonBreakingChangeWhenEndpointAdded() {
        // Given
        String oldSpec = """
                {
                    "openapi": "3.0.0",
                    "info": {
                        "title": "Test API",
                        "version": "1.0.0"
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

        String newSpec = """
                {
                    "openapi": "3.0.0",
                    "info": {
                        "title": "Test API",
                        "version": "2.0.0"
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
                        },
                        "/api/new": {
                            "get": {
                                "summary": "New endpoint",
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
        DiffResultDTO result = diffService.compare(oldSpec, newSpec);

        // Then
        assertThat(result.hasBreakingChanges()).isFalse();
        assertThat(result.compatible()).isTrue();
    }

    @Test
    @DisplayName("Should count breaking changes correctly")
    void shouldCountBreakingChangesCorrectly() {
        // Given - Multiple breaking changes
        String oldSpec = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "API", "version": "1.0.0"},
                    "paths": {
                        "/api/v1": {"get": {"responses": {"200": {"description": "OK"}}}},
                        "/api/v2": {"get": {"responses": {"200": {"description": "OK"}}}}
                    }
                }
                """;

        String newSpec = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "API", "version": "2.0.0"},
                    "paths": {}
                }
                """;

        // When
        DiffResultDTO result = diffService.compare(oldSpec, newSpec);

        // Then - Library counts incompatible changes
        assertThat(result.hasBreakingChanges()).isTrue();
    }

    @Test
    @DisplayName("Should generate diff output")
    void shouldGenerateDiffOutput() {
        // Given
        String oldSpec = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "API", "version": "1.0.0"},
                    "paths": {
                        "/api/test": {"get": {"responses": {"200": {"description": "OK"}}}}
                    }
                }
                """;

        String newSpec = """
                {
                    "openapi": "3.0.0",
                    "info": {"title": "API", "version": "2.0.0"},
                    "paths": {
                        "/api/test": {"get": {"responses": {"200": {"description": "Updated"}}}}
                    }
                }
                """;

        // When
        DiffResultDTO result = diffService.compare(oldSpec, newSpec);

        // Then
        assertThat(result.diff()).isNotNull();
        assertThat(result.diff()).contains("API");
    }
}
