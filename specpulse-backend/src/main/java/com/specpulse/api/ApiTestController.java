package com.specpulse.api;

import com.specpulse.test.ApiTestService;
import com.specpulse.test.TestExecutionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tests")
public class ApiTestController {

    private final ApiTestService testService;

    public ApiTestController(ApiTestService testService) {
        this.testService = testService;
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<TestExecutionDTO>> getTestExecutionsByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(testService.getTestExecutionsByServiceId(serviceId));
    }

    @GetMapping("/version/{specVersionId}")
    public ResponseEntity<List<TestExecutionDTO>> getTestExecutionsByVersion(@PathVariable Long specVersionId) {
        return ResponseEntity.ok(testService.getTestExecutionsBySpecVersionId(specVersionId));
    }
}
