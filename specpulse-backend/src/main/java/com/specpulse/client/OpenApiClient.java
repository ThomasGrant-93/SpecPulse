package com.specpulse.client;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenApiClient.class);

    private final RequestConfig requestConfig;

    public OpenApiClient() {
        this.requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .build();
    }

    public SpecFetchResult fetchSpec(String url) {
        log.debug("Fetching OpenAPI spec from: {}", url);
        long startTime = System.currentTimeMillis();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json, application/x-yaml, */*");

            return httpClient.execute(request, response -> {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = response.getCode();

                if (statusCode >= 200 && statusCode < 300) {
                    String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    log.info("Successfully fetched spec from {} (status: {}, size: {} bytes, duration: {}ms)",
                            url, statusCode, content.length(), duration);
                    return SpecFetchResult.success(content, statusCode, duration);
                } else {
                    String errorBody = response.getEntity() != null
                            ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)
                            : "";
                    log.warn("Failed to fetch spec from {} (status: {}, error: {})", url, statusCode, errorBody);
                    return SpecFetchResult.failure(statusCode, errorBody, duration);
                }
            });

        } catch (java.io.IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error fetching spec from {}: {}", url, e.getMessage(), e);
            return SpecFetchResult.error(e.getMessage(), duration);
        }
    }

    /**
     * Validate OpenAPI spec URL and content
     *
     * @param url the URL to validate
     * @return ValidationResult with success status and any errors
     */
    public ValidationResult validateOpenApiSpec(String url) {
        log.info("Validating OpenAPI spec from: {}", url);
        List<String> errors = new ArrayList<>();

        SpecFetchResult result = fetchSpec(url);

        if (!result.success()) {
            errors.add("Failed to fetch spec: " + result.errorMessage());
            return new ValidationResult(false, errors);
        }

        // Check if content is valid JSON
        String content = result.content();
        if (content == null || content.trim().isEmpty()) {
            errors.add("Empty response from URL");
            return new ValidationResult(false, errors);
        }

        // Try to parse as JSON to validate format
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(content);

            // Check if this is Swagger 2.0 (not supported)
            if (jsonNode.has("swagger") && !jsonNode.has("openapi")) {
                errors.add("Swagger 2.0 specifications are not supported. Please convert your specification to OpenAPI 3.0 or later. " +
                        "You can use online converters like https://editor.swagger.io/ to convert Swagger 2.0 to OpenAPI 3.0.");
                return new ValidationResult(false, errors);
            }

            // Check for required OpenAPI 3.x fields
            if (!jsonNode.has("openapi")) {
                errors.add("Missing required field: 'openapi'. This doesn't appear to be a valid OpenAPI specification. " +
                        "Note: Swagger 2.0 is not supported. Please use OpenAPI 3.0 or later.");
            }

            if (!jsonNode.has("info")) {
                errors.add("Missing required field: 'info'. OpenAPI spec must contain metadata about the API.");
            } else {
                com.fasterxml.jackson.databind.JsonNode info = jsonNode.get("info");
                if (!info.has("title")) {
                    errors.add("Missing required field: 'info.title'. API must have a title.");
                }
                if (!info.has("version")) {
                    errors.add("Missing required field: 'info.version'. API must have a version.");
                }
            }

            if (!jsonNode.has("paths")) {
                errors.add("Missing required field: 'paths'. OpenAPI spec must define API endpoints.");
            } else if (!jsonNode.get("paths").isObject()) {
                errors.add("Invalid field: 'paths' must be an object.");
            }

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            errors.add("Invalid JSON format: " + e.getOriginalMessage());
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public record ValidationResult(
            boolean success,
            List<String> errors
    ) {
    }
}
