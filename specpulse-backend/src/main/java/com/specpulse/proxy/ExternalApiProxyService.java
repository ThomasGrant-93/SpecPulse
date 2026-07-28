package com.specpulse.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.specpulse.client.OutboundUrlSecurity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class ExternalApiProxyService {

    private static final Set<String> BLOCKED_REQUEST_HEADERS = Set.of(
            "host",
            "connection",
            "content-length",
            "transfer-encoding",
            "expect",
            "upgrade",
            "proxy-connection"
    );

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final OutboundUrlSecurity outboundUrlSecurity;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final long maxRequestBodyBytes;

    public ExternalApiProxyService(OutboundUrlSecurity outboundUrlSecurity, ObjectMapper objectMapper) {
        this.outboundUrlSecurity = outboundUrlSecurity;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Best-effort request-body cap to prevent memory/CPU DoS.
        // Response parsing is also capped separately.
        this.maxRequestBodyBytes = 2_000_000L;
    }

    public ExternalApiProxyResponse proxy(ExternalApiProxyRequest request) {
        try {
            validateRequest(request);

            String method = request.method().trim().toUpperCase(Locale.ROOT);
            URI uri = URI.create(request.url());

            Map<String, String> safeHeaders = sanitizeHeaders(request.headers());

            String contentType = safeHeaders.getOrDefault("Content-Type", safeHeaders.getOrDefault("content-type", "application/json"));
            boolean isJson = isJsonContentType(contentType);

            HttpRequest.BodyPublisher bodyPublisher;
            if (request.body() == null || request.body().isNull() || "GET".equals(method)) {
                bodyPublisher = HttpRequest.BodyPublishers.noBody();
            } else {
                String outgoingBody;
                if (isJson) {
                    outgoingBody = request.body().toString();
                } else {
                    // For non-json, prefer raw text when the client sent a text node.
                    outgoingBody = request.body().isTextual() ? request.body().asText() : request.body().toString();
                }

                long bodyBytes = outgoingBody.getBytes(StandardCharsets.UTF_8).length;
                if (bodyBytes > maxRequestBodyBytes) {
                    throw new IllegalArgumentException("Request body is too large");
                }

                bodyPublisher = HttpRequest.BodyPublishers.ofString(outgoingBody, charsetFromContentType(contentType));

                // Ensure there's a Content-Type if the caller provided a body.
                if (safeHeaders.keySet().stream().noneMatch(k -> k.equalsIgnoreCase("Content-Type"))) {
                    safeHeaders.put("Content-Type", contentType);
                }
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(30))
                    .method(method, bodyPublisher);

            // Avoid sending Accept-Encoding control headers from the browser.
            for (Map.Entry<String, String> h : safeHeaders.entrySet()) {
                if (h.getValue() == null) {
                    continue;
                }
                builder.header(h.getKey(), h.getValue());
            }

            long start = System.currentTimeMillis();
            HttpResponse<InputStream> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            long duration = System.currentTimeMillis() - start;

            String responseContentType = response.headers().firstValue("Content-Type").orElse("");
            JsonNode responseBody = readAndParseBody(response, responseContentType);

            Map<String, String> responseHeaders = new HashMap<>();
            for (Map.Entry<String, List<String>> e : response.headers().map().entrySet()) {
                responseHeaders.put(e.getKey(), String.join(",", e.getValue()));
            }

            String statusText = statusText(response.statusCode());
            // duration is intentionally not exposed by this proxy response; frontend measures its own duration.
            return new ExternalApiProxyResponse(
                    response.statusCode(),
                    statusText,
                    responseHeaders,
                    responseBody,
                    null
            );
        } catch (Exception e) {
            return new ExternalApiProxyResponse(
                    0,
                    "ERROR",
                    Map.of(),
                    NullNode.getInstance(),
                    e.getMessage()
            );
        }
    }

    private void validateRequest(ExternalApiProxyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.url() == null || request.url().isBlank()) {
            throw new IllegalArgumentException("url is required");
        }
        if (request.method() == null || request.method().isBlank()) {
            throw new IllegalArgumentException("method is required");
        }

        outboundUrlSecurity.validateOpenApiUrl(request.url());

        String method = request.method().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_METHODS.contains(method)) {
            throw new IllegalArgumentException("Unsupported method: " + request.method());
        }
    }

    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return Map.of();
        }

        Map<String, String> out = new HashMap<>();
        int count = 0;

        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (count >= 30) {
                break;
            }

            String key = e.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            key = key.trim();
            if (!key.matches("^[A-Za-z0-9-]+$")) {
                continue;
            }
            if (BLOCKED_REQUEST_HEADERS.contains(key.toLowerCase(Locale.ROOT))) {
                continue;
            }

            String value = e.getValue();
            if (value == null) {
                continue;
            }
            if (value.length() > 8192) {
                continue;
            }

            out.put(key, value);
            count++;
        }

        return out;
    }

    private boolean isJsonContentType(String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return ct.startsWith("application/json") || ct.contains("+json");
    }

    private Charset charsetFromContentType(String contentType) {
        if (contentType == null) {
            return StandardCharsets.UTF_8;
        }

        String[] parts = contentType.split(";");
        for (String p : parts) {
            String part = p.trim();
            if (part.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                String charset = part.substring("charset=".length()).trim();
                try {
                    return Charset.forName(charset);
                } catch (Exception ignored) {
                    return StandardCharsets.UTF_8;
                }
            }
        }

        return StandardCharsets.UTF_8;
    }

    private JsonNode readAndParseBody(HttpResponse<InputStream> response, String responseContentType) {
        // Best-effort limit: prevent unbounded memory usage.
        long maxBytes = 2_000_000;
        try (InputStream is = response.body()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int read;
            while ((read = is.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException("Response body is too large");
                }
                baos.write(buffer, 0, read);
            }

            if (baos.size() == 0) {
                return NullNode.getInstance();
            }

            Charset charset = charsetFromContentType(responseContentType);
            String body = baos.toString(charset);
            String ct = responseContentType == null ? "" : responseContentType.toLowerCase(Locale.ROOT);
            if (ct.startsWith("application/json") || ct.contains("+json")) {
                try {
                    return objectMapper.readTree(body);
                } catch (Exception ignored) {
                    return TextNode.valueOf(body);
                }
            }

            return TextNode.valueOf(body);
        } catch (IOException e) {
            return TextNode.valueOf("[proxy read error] " + e.getMessage());
        }
    }

    private String statusText(int statusCode) {
        try {
            return HttpStatus.valueOf(statusCode).getReasonPhrase();
        } catch (Exception ignored) {
            return "";
        }
    }
}
