package com.specpulse.api;

import com.specpulse.api.mapper.RegistryApiMapper;
import com.specpulse.registry.RegistryService;
import com.specpulse.registry.ServiceDTO;
import com.specpulse.registry.ServiceWithVersionDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/registry")
public class RegistryController {

    private final RegistryService registryService;
    private final RegistryApiMapper registryApiMapper;

    public RegistryController(
            RegistryService registryService,
            RegistryApiMapper registryApiMapper
    ) {
        this.registryService = registryService;
        this.registryApiMapper = registryApiMapper;
    }

    @GetMapping
    public ResponseEntity<List<ServiceWithVersionDTO>> getAllServices() {
        return ResponseEntity.ok(registryService.getAllServicesWithVersions());
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<ServiceDTO>> getEnabledServices() {
        return ResponseEntity.ok(registryService.getEnabledServices());
    }

    @GetMapping("/search")
    public ResponseEntity<List<ServiceDTO>> searchServices(
            @RequestParam String q) {
        return ResponseEntity.ok(registryService.searchServices(q));
    }

    @GetMapping("/search/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @RequestParam String q) {
        return ResponseEntity.ok(registryService.getSearchSuggestions(q));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validateService(
            @RequestBody ValidateRequest request
    ) {
        var serviceRequest = registryApiMapper.toServiceValidationRequest(request);
        var result = registryService.validateService(serviceRequest);

        return ResponseEntity.ok(registryApiMapper.toValidateResponse(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceDTO> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(registryService.getServiceById(id));
    }

    @PostMapping
    public ResponseEntity<ServiceDTO> createService(
            @Valid @RequestBody RegistryService.CreateServiceRequest request) {
        ServiceDTO created = registryService.createService(request);
        return ResponseEntity.created(URI.create("/api/v1/registry/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceDTO> updateService(
            @PathVariable Long id,
            @Valid @RequestBody RegistryService.UpdateServiceRequest request) {
        return ResponseEntity.ok(registryService.updateService(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id
    ) {
        registryService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    public record ValidateRequest(
            String name,
            String openApiUrl
    ) {
    }

    public record ValidateResponse(
            boolean valid,
            List<String> errors
    ) {
    }
}
