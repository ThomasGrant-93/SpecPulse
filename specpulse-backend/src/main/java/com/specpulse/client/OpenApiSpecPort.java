package com.specpulse.client;

public interface OpenApiSpecPort {

    SpecFetchResult fetchSpec(String url);

    OpenApiValidationResult validateOpenApiSpec(String url);
}
