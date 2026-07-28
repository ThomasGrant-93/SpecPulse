package com.specpulse.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenApiClient implements OpenApiSpecPort {

    private static final Logger log = LoggerFactory.getLogger(OpenApiClient.class);
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final RequestConfig requestConfig;

    private final OutboundUrlSecurity outboundUrlSecurity;

    private final long maxSpecBytes;

    public OpenApiClient(
            OutboundUrlSecurity outboundUrlSecurity,
            @Value("${specpulse.outbound.max-spec-bytes:2097152}") long maxSpecBytes
    ) {
        this.outboundUrlSecurity = outboundUrlSecurity;
        this.maxSpecBytes = maxSpecBytes;

        this.requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(30))
                .setRedirectsEnabled(false)
                .build();
    }

    @Override
    public SpecFetchResult fetchSpec(String url) {
        outboundUrlSecurity.validateOpenApiUrl(url);
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

                if (response.getEntity() == null) {
                    return SpecFetchResult.failure(statusCode, "Empty response body", duration);
                }

                String body;
                try {
                    body = readBodyWithLimit(response, maxSpecBytes);
                } catch (IllegalArgumentException e) {
                    return SpecFetchResult.failure(statusCode, e.getMessage(), duration);
                }

                if (statusCode >= 200 && statusCode < 300) {
                    String content = body;
                    log.info("Successfully fetched spec from {} (status: {}, size: {} bytes, duration: {}ms)",
                            url, statusCode, content.length(), duration);
                    return SpecFetchResult.success(content, statusCode, duration);
                } else {
                    log.warn("Failed to fetch spec from {} (status: {}, error body length: {})",
                            url, statusCode, body != null ? body.length() : 0);
                    return SpecFetchResult.failure(statusCode, body, duration);
                }
            });

        } catch (java.io.IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error fetching spec from {}: {}", url, e.getMessage(), e);
            return SpecFetchResult.error(e.getMessage(), duration);
        }
    }

    /**
     * Validate OpenAPI spec URL and content.
     *
     * @param url the URL to validate.
     * @return ValidationResult with success status and any errors.
     */
    @Override
    public OpenApiValidationResult validateOpenApiSpec(String url) {
        log.info("Validating OpenAPI spec from: {}", url);
        List<String> errors = new ArrayList<>();

        SpecFetchResult result = fetchSpec(url);

        if (!result.success()) {
            errors.add("Failed to fetch spec: " + result.errorMessage());
            return new OpenApiValidationResult(false, errors);
        }

        // Check if content is valid JSON
        String content = result.content();
        if (content == null || content.trim().isEmpty()) {
            errors.add("Empty response from URL");
            return new OpenApiValidationResult(false, errors);
        }

        // Try to parse as JSON or YAML to validate format
        try {
            JsonNode jsonNode = parseSpecTree(content);

            boolean isSwagger2 = jsonNode.has("swagger") && !jsonNode.has("openapi");

            if (isSwagger2) {
                // Swagger 2.0 required fields (minimal validation): swagger, info.title, info.version, paths
                if (!jsonNode.has("info")) {
                    errors.add("Missing required field: 'info'. Swagger spec must contain API metadata.");
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
                    errors.add("Missing required field: 'paths'. Swagger spec must define API endpoints.");
                } else if (!jsonNode.get("paths").isObject()) {
                    errors.add("Invalid field: 'paths' must be an object.");
                }

            } else {
                // OpenAPI 3.x required fields
                if (!jsonNode.has("openapi")) {
                    errors.add("Missing required field: 'openapi'. This doesn't appear to be a valid OpenAPI 3.x or Swagger 2.0 specification.");
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
            }

        } catch (JsonProcessingException e) {
            errors.add("Invalid OpenAPI format (expected JSON or YAML): " + e.getOriginalMessage());
        }

        return new OpenApiValidationResult(errors.isEmpty(), errors);
    }

    private JsonNode parseSpecTree(String content) throws JsonProcessingException {
        try {
            return JSON_MAPPER.readTree(content);
        } catch (JsonProcessingException ignored) {
            return YAML_MAPPER.readTree(content);
        }
    }

    private String readBodyWithLimit(ClassicHttpResponse response, long maxBytes) throws IOException {
        try (InputStream is = response.getEntity().getContent()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            int read;
            while ((read = is.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("OpenAPI spec is too large");
                }
                baos.write(buffer, 0, read);
            }

            return baos.toString(StandardCharsets.UTF_8);
        }
    }

}
