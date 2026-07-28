package com.specpulse.api;

import com.specpulse.scheduler.PullAllResult;
import com.specpulse.scheduler.PullResult;
import com.specpulse.scheduler.SpecPullScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for manual pull operations.
 */
@RestController
@RequestMapping("/api/v1/pull")
public class PullController {

    private final SpecPullScheduler pullScheduler;

    // If configured (non-empty), requires Authorization: Bearer <token> for pull operations.
    private final String authToken;

    public PullController(
            SpecPullScheduler pullScheduler,
            @Value("${specpulse.auth.token:}") String authToken
    ) {
        this.pullScheduler = pullScheduler;
        this.authToken = authToken;
    }

    private boolean isPullAuthorized(String authorization) {
        if (authToken == null || authToken.isBlank()) {
            return true; // Backward-compatible default.
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        return authToken.equals(token);
    }

    /**
     * Manually trigger pull for a specific service.
     */
    @PostMapping("/service/{serviceId}")
    public ResponseEntity<PullResult> pullService(
            @PathVariable Long serviceId,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        if (!isPullAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PullResult result = pullScheduler.pullServiceById(serviceId);
        return ResponseEntity.ok(result);
    }

    /**
     * Manually trigger pull for all enabled services.
     */
    @PostMapping("/all")
    public ResponseEntity<PullAllResult> pullAll(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        if (!isPullAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PullAllResult result = pullScheduler.pullAllEnabledServicesManual();
        return ResponseEntity.ok(result);
    }
}
