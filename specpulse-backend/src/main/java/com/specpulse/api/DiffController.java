package com.specpulse.api;

import com.specpulse.diff.DiffResultDTO;
import com.specpulse.diff.DiffService;
import com.specpulse.diff.SpecDiffDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diffs")
public class DiffController {

    private final DiffService diffService;

    public DiffController(DiffService diffService) {
        this.diffService = diffService;
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
            @RequestBody CompareSpecsRequest request) {
        DiffResultDTO result = diffService.compare(request.oldSpec(), request.newSpec());
        return ResponseEntity.ok(result);
    }

    public record CompareSpecsRequest(
            String oldSpec,
            String newSpec
    ) {
    }
}
