package com.specpulse.api;

import com.specpulse.version.SpecVersionDTO;
import com.specpulse.version.VersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/versions")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<SpecVersionDTO>> getVersionsByService(@PathVariable Long serviceId) {
        List<SpecVersionDTO> versions = versionService.getVersionsByServiceId(serviceId).stream()
                .map(SpecVersionDTO::withoutContent)
                .toList();
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/service/{serviceId}/latest")
    public ResponseEntity<SpecVersionDTO> getLatestVersion(@PathVariable Long serviceId) {
        return versionService.getLatestVersion(serviceId)
                .map(dto -> ResponseEntity.ok(dto.withoutContent()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecVersionDTO> getVersionById(
            @PathVariable Long id,
            @RequestParam(name = "diff_only", required = false, defaultValue = "false") boolean diffOnly,
            @RequestParam(name = "exclude_unchanged", required = false, defaultValue = "false") boolean excludeUnchanged,
            @RequestParam(name = "compare_to", required = false) Long compareTo
    ) {
        boolean filteringRequested = diffOnly || excludeUnchanged;
        if (!filteringRequested) {
            // Default: return full spec content for the spec viewer.
            return ResponseEntity.ok(versionService.getVersionById(id));
        }

        if (compareTo == null) {
            // Filtering requires comparing two versions.
            throw new IllegalArgumentException("compare_to is required when diff_only or exclude_unchanged is true");
        }

        return ResponseEntity.ok(versionService.getVersionByIdDiffOnly(id, compareTo));
    }

    @GetMapping("/{id}/raw")
    public ResponseEntity<String> getVersionRawContent(@PathVariable Long id) {
        String content = versionService.getVersionById(id).specContent();
        return ResponseEntity.ok(content);
    }
}
