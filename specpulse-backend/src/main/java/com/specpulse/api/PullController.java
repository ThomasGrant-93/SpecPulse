package com.specpulse.api;

import com.specpulse.scheduler.PullAllResult;
import com.specpulse.scheduler.PullResult;
import com.specpulse.scheduler.SpecPullScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for manual pull operations
 */
@RestController
@RequestMapping("/api/v1/pull")
public class PullController {

    private final SpecPullScheduler pullScheduler;

    public PullController(SpecPullScheduler pullScheduler) {
        this.pullScheduler = pullScheduler;
    }

    /**
     * Manually trigger pull for a specific service
     */
    @PostMapping("/service/{serviceId}")
    public ResponseEntity<PullResult> pullService(@PathVariable Long serviceId) {
        PullResult result = pullScheduler.pullServiceById(serviceId);
        return ResponseEntity.ok(result);
    }

    /**
     * Manually trigger pull for all enabled services
     */
    @PostMapping("/all")
    public ResponseEntity<PullAllResult> pullAll() {
        PullAllResult result = pullScheduler.pullAllEnabledServicesManual();
        return ResponseEntity.ok(result);
    }
}
