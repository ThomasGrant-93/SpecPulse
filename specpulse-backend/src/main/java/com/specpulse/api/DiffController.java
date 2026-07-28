package com.specpulse.api;

import com.specpulse.diff.DiffResultDTO;
import com.specpulse.diff.DiffService;
import com.specpulse.diff.SpecDiffDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diffs")
public class DiffController {

    private final DiffService diffService;

    // If configured (non-empty), requires Authorization: Bearer <token> for diff comparison.
    private final String authToken;

    public DiffController(
            DiffService diffService,
            @Value("${specpulse.auth.token:}") String authToken
    ) {
        this.diffService = diffService;
        this.authToken = authToken;
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<SpecDiffDTO>> getDiffsByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(diffService.getDiffsByServiceId(serviceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecDiffDTO> getDiffById(@PathVariable Long id) {
        return ResponseEntity.ok(diffService.getDiffById(id));
    }

    @PostMapping("/compare")
    public ResponseEntity<DiffResultDTO> compareSpecs(
            @RequestBody CompareSpecsRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        if (authToken != null && !authToken.isBlank()) {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String token = authorization.substring("Bearer ".length()).trim();
            if (!authToken.equals(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        DiffResultDTO result = diffService.compare(request.oldSpec(), request.newSpec());
        return ResponseEntity.ok(result);
    }

    public record CompareSpecsRequest(
            String oldSpec,
            String newSpec
    ) {
    }
}
