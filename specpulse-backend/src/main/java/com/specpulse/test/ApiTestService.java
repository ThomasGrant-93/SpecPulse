package com.specpulse.test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ApiTestService {

    private static final Logger log = LoggerFactory.getLogger(ApiTestService.class);

    private final TestExecutionRepository repository;
    private final HttpClient httpClient;

    public ApiTestService(TestExecutionRepository repository) {
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Generate test cases from OpenAPI spec
     */
    public List<TestCase> generateTestCases(OpenAPI openAPI, String baseUrl) {
        List<TestCase> testCases = new ArrayList<>();

        if (openAPI.getPaths() == null) {
            return testCases;
        }

        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();

            // Test each HTTP method
            Map<String, Operation> operations = Map.of(
                    "GET", pathItem.getGet(),
                    "POST", pathItem.getPost(),
                    "PUT", pathItem.getPut(),
                    "DELETE", pathItem.getDelete(),
                    "PATCH", pathItem.getPatch()
            );

            for (Map.Entry<String, Operation> opEntry : operations.entrySet()) {
                String method = opEntry.getKey();
                Operation operation = opEntry.getValue();

                if (operation != null) {
                    String testId = method.toLowerCase() + "_" + path.replaceAll("/", "_").replaceAll("\\{", "").replaceAll("\\}", "");
                    testCases.add(new TestCase(testId, method, baseUrl + path, operation));
                }
            }
        }

        log.info("Generated {} test cases from OpenAPI spec", testCases.size());
        return testCases;
    }

    /**
     * Run a single test case
     */
    public TestExecutionDTO runTest(TestCase testCase, Long serviceId, Long specVersionId) {
        log.info("Running test: {} {} {}", testCase.testId(), testCase.method(), testCase.url());
        Instant startTime = Instant.now();

        TestExecutionEntity execution = new TestExecutionEntity(
                serviceId,
                specVersionId,
                testCase.testId(),
                testCase.url(),
                testCase.method(),
                "RUNNING",
                startTime
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(testCase.url()))
                    .timeout(Duration.ofSeconds(30))
                    .method(testCase.method(), HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long duration = Duration.between(startTime, Instant.now()).toMillis();
            int statusCode = response.statusCode();

            execution.setStatus(statusCode >= 200 && statusCode < 400 ? "PASSED" : "FAILED");
            execution.setResponseCode(statusCode);
            execution.setResponseTimeMs(duration);

            if (statusCode >= 400) {
                execution.setErrorMessage("HTTP " + statusCode + ": " + response.body().substring(0, Math.min(500, response.body().length())));
            }

        } catch (java.io.IOException | InterruptedException e) {
            execution.setStatus("ERROR");
            execution.setErrorMessage(e.getMessage());
            log.error("Test execution error: {}", e.getMessage(), e);
        }

        TestExecutionEntity saved = repository.save(execution);
        return TestExecutionDTO.fromEntity(saved);
    }

    /**
     * Run all tests for a service
     */
    public List<TestExecutionDTO> runAllTests(OpenAPI openAPI, String baseUrl, Long serviceId, Long specVersionId) {
        List<TestCase> testCases = generateTestCases(openAPI, baseUrl);
        List<TestExecutionDTO> results = new ArrayList<>();

        for (TestCase testCase : testCases) {
            results.add(runTest(testCase, serviceId, specVersionId));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<TestExecutionDTO> getTestExecutionsByServiceId(Long serviceId) {
        return repository.findByServiceIdOrderByExecutedAtDesc(serviceId).stream()
                .map(TestExecutionDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TestExecutionDTO> getTestExecutionsBySpecVersionId(Long specVersionId) {
        return repository.findBySpecVersionIdOrderByExecutedAtDesc(specVersionId).stream()
                .map(TestExecutionDTO::fromEntity)
                .toList();
    }

    public record TestCase(
            String testId,
            String method,
            String url,
            Operation operation
    ) {
    }
}
