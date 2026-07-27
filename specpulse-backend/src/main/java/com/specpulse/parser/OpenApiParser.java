package com.specpulse.parser;

import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenApiParser {

    private static final Logger log = LoggerFactory.getLogger(OpenApiParser.class);

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    @Value("${specpulse.outbound.max-spec-bytes:2097152}")
    private long maxSpecBytes = 2_097_152L;

    public ParseResult parse(String specContent) {
        if (specContent == null || specContent.isBlank()) {
            return ParseResult.failure("OpenAPI spec content is empty");
        }

        // Prevent CPU/memory DoS from very large specs.
        if (specContent.length() > maxSpecBytes) {
            return ParseResult.failure("OpenAPI spec is too large");
        }

        log.debug("Parsing OpenAPI specification, content length: {}", specContent.length());

        ParseOptions options = new ParseOptions();
        // Keep default resolution behavior so parsing produces a valid OpenAPI model.
        // SSRF is mitigated by URL allow/block rules and redirects disabled in the fetch path.
        options.setResolve(true);
        options.setResolveFully(true);

        // Check if this is Swagger 2.0 or OpenAPI 3.x
        boolean isSwagger2 = specContent.contains("\"swagger\"");
        boolean isOpenAPI3 = specContent.contains("\"openapi\"");

        if (isSwagger2) {
            log.info("Detected Swagger 2.0 content in OpenAPI 3.x spec, attempting conversion...");
        }

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent, null, options);

        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            if (isSwagger2) {
                // Swagger 2.0 fallback: we may not always be able to convert to OAS3.
                // If the Swagger 2.0 document looks structurally valid, store it as-is.
                List<String> swagger2Errors = validateSwagger2RequiredFields(specContent);
                if (!swagger2Errors.isEmpty()) {
                    String errorMsg = "Invalid Swagger 2.0 specification: " + String.join("; ", swagger2Errors);
                    log.error("Failed to parse OpenAPI spec: {}", errorMsg);
                    return ParseResult.failure(errorMsg);
                }

                JsonNode node;
                try {
                    node = parseSpecTree(specContent);
                } catch (Exception e) {
                    return ParseResult.failure("Invalid Swagger 2.0 format");
                }

                String title = node.path("info").path("title").asText(null);
                String specVersion = node.path("swagger").asText("2.0");
                String contentHash = computeHash(specContent);

                log.info("Stored Swagger 2.0 spec as-is: title='{}', version='{}', hash='{}'",
                        title, specVersion, contentHash);

                return ParseResult.success(null, specVersion, title, specContent, contentHash);
            }

            String errorMsg = result.getMessages() != null && !result.getMessages().isEmpty()
                    ? String.join("; ", result.getMessages())
                    : "Unknown parsing error";
            log.error("Failed to parse OpenAPI spec: {}", errorMsg);
            return ParseResult.failure(errorMsg);
        }

        // Validate required fields according to OpenAPI 3.0 specification
        List<String> validationErrors = validateRequiredFields(openAPI, specContent);
        if (!validationErrors.isEmpty()) {
            String errorMsg = "Invalid OpenAPI specification: " + String.join("; ", validationErrors) +
                    ". Please ensure your specification contains all required fields according to OpenAPI 3.0 specification.";
            log.error("Failed to parse OpenAPI spec: {}", errorMsg);
            return ParseResult.failure(errorMsg);
        }

        String specVersion = openAPI.getOpenapi();
        String title = openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : null;

        boolean shouldNormalizeToOpenApi3Json = isSwagger2 && !isOpenAPI3;
        String contentToStore = specContent;
        String contentHash = computeHash(specContent);

        if (shouldNormalizeToOpenApi3Json) {
            // Use swagger-parser's converted OpenAPI model for consistent storage + diffing.
            try {
                contentToStore = JSON_MAPPER.writeValueAsString(openAPI);
                contentHash = computeHash(contentToStore);
            } catch (Exception e) {
                log.warn("Failed to serialize converted Swagger 2.0 to OpenAPI 3 JSON; storing original content. error={}", e.getMessage());
            }
        }

        log.info("Successfully parsed OpenAPI spec: title='{}', version='{}', hash='{}'",
                title, specVersion, contentHash);

        return ParseResult.success(openAPI, specVersion, title, contentToStore, contentHash);
    }

    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Validates required fields according to OpenAPI 3.0 specification.
     * Required fields:
     * - openapi (version string)
     * - info object containing:
     * - title (string)
     * - version (string)
     */
    private java.util.List<String> validateRequiredFields(OpenAPI openAPI, String specContent) {
        java.util.List<String> errors = new java.util.ArrayList<>();

        // Check if openapi version is present (should always be present if parsing succeeded)
        if (openAPI.getOpenapi() == null || openAPI.getOpenapi().isBlank()) {
            errors.add("Missing required field 'openapi' (specification version)");
        }

        // Check if info object is present
        if (openAPI.getInfo() == null) {
            errors.add("Missing required 'info' object");
        } else {
            // Check if info.title is present
            if (openAPI.getInfo().getTitle() == null || openAPI.getInfo().getTitle().isBlank()) {
                errors.add("Missing required field 'info.title'");
            }

            // Check if info.version is present
            if (openAPI.getInfo().getVersion() == null || openAPI.getInfo().getVersion().isBlank()) {
                errors.add("Missing required field 'info.version'");
            }
        }

        return errors;
    }

    private List<String> validateSwagger2RequiredFields(String specContent) {
        List<String> errors = new ArrayList<>();

        JsonNode node;
        try {
            node = parseSpecTree(specContent);
        } catch (Exception e) {
            return List.of("Invalid JSON or YAML format");
        }

        if (!node.has("swagger")) {
            errors.add("Missing required field: 'swagger'.");
        }

        if (!node.has("info")) {
            errors.add("Missing required field: 'info'.");
        } else {
            JsonNode info = node.get("info");
            if (!info.has("title")) {
                errors.add("Missing required field: 'info.title'.");
            }
            if (!info.has("version")) {
                errors.add("Missing required field: 'info.version'.");
            }
        }

        if (!node.has("paths")) {
            errors.add("Missing required field: 'paths'.");
        } else if (!node.get("paths").isObject()) {
            errors.add("Invalid field: 'paths' must be an object.");
        }

        return errors;
    }

    private JsonNode parseSpecTree(String content) throws Exception {
        try {
            return JSON_MAPPER.readTree(content);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) {
            return YAML_MAPPER.readTree(content);
        }
    }

    public record ParseResult(
            boolean success,
            OpenAPI openAPI,
            String specVersion,
            String title,
            String content,
            String contentHash,
            String errorMessage
    ) {
        public static ParseResult success(OpenAPI openAPI, String specVersion, String title, String content, String hash) {
            return new ParseResult(true, openAPI, specVersion, title, content, hash, null);
        }

        public static ParseResult failure(String errorMessage) {
            return new ParseResult(false, null, null, null, null, null, errorMessage);
        }
    }
}
