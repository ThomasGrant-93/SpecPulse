package com.specpulse.parser;

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

@Component
public class OpenApiParser {

    private static final Logger log = LoggerFactory.getLogger(OpenApiParser.class);

    public ParseResult parse(String specContent) {
        log.debug("Parsing OpenAPI specification, content length: {}", specContent.length());

        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);

        // Check if this is Swagger 2.0 or OpenAPI 3.x
        boolean isSwagger2 = specContent.contains("\"swagger\"");
        boolean isOpenAPI3 = specContent.contains("\"openapi\"");

        if (isSwagger2 && !isOpenAPI3) {
            String errorMsg = "Swagger 2.0 specifications are not supported. " +
                    "Please convert your specification to OpenAPI 3.0 or later. " +
                    "You can use online converters like https://editor.swagger.io/ to convert Swagger 2.0 to OpenAPI 3.0.";
            log.error("Failed to parse OpenAPI spec: {}", errorMsg);
            return ParseResult.failure(errorMsg);
        }

        if (isSwagger2) {
            log.info("Detected Swagger 2.0 content in OpenAPI 3.x spec, attempting conversion...");
        }

        SwaggerParseResult result = new OpenAPIV3Parser().readContents(specContent, null, options);

        if (result.getOpenAPI() == null) {
            String errorMsg;
            if (isSwagger2) {
                // Filter out "attribute openapi is missing" as it's expected for Swagger 2.0
                java.util.List<String> filteredMessages = result.getMessages() != null
                        ? result.getMessages().stream()
                        .filter(msg -> !msg.contains("attribute openapi is missing"))
                        .collect(java.util.stream.Collectors.toList())
                        : java.util.List.of();

                if (filteredMessages.isEmpty()) {
                    errorMsg = "Failed to convert Swagger 2.0 to OpenAPI 3.0. " +
                            "This may be due to invalid Swagger 2.0 format. " +
                            "Please validate your spec at https://editor.swagger.io/";
                } else {
                    errorMsg = "Failed to convert Swagger 2.0 to OpenAPI 3.0. Errors: " +
                            String.join(", ", filteredMessages) + ". " +
                            "Please validate your spec at https://editor.swagger.io/";
                }
            } else {
                errorMsg = result.getMessages() != null && !result.getMessages().isEmpty()
                        ? String.join("; ", result.getMessages())
                        : "Unknown parsing error";
            }
            log.error("Failed to parse OpenAPI spec: {}", errorMsg);
            return ParseResult.failure(errorMsg);
        }

        OpenAPI openAPI = result.getOpenAPI();

        // Validate required fields according to OpenAPI 3.0 specification
        // Required fields: openapi, info (with title and version)
        java.util.List<String> validationErrors = validateRequiredFields(openAPI, specContent);
        if (!validationErrors.isEmpty()) {
            String errorMsg = "Invalid OpenAPI specification: " + String.join("; ", validationErrors) +
                    ". Please ensure your specification contains all required fields according to OpenAPI 3.0 specification.";
            log.error("Failed to parse OpenAPI spec: {}", errorMsg);
            return ParseResult.failure(errorMsg);
        }

        String specVersion = openAPI.getOpenapi();
        String title = openAPI.getInfo() != null ? openAPI.getInfo().getTitle() : null;

        String contentHash = computeHash(specContent);

        log.info("Successfully parsed OpenAPI spec: title='{}', version='{}', hash='{}'",
                title, specVersion, contentHash);

        return ParseResult.success(openAPI, specVersion, title, specContent, contentHash);
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
