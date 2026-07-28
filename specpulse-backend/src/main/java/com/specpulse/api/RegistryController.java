package com.specpulse.api;

import com.specpulse.api.mapper.RegistryApiMapper;
import com.specpulse.registry.RegistryService;
import com.specpulse.registry.ServiceDTO;
import com.specpulse.registry.ServiceWithVersionDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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

    // If configured (non-empty), requires Authorization: Bearer <token> for registry mutations.
    private final String authToken;

    public RegistryController(
            RegistryService registryService,
            RegistryApiMapper registryApiMapper,
            @Value("${specpulse.auth.token:}") String authToken
    ) {
        this.registryService = registryService;
        this.registryApiMapper = registryApiMapper;
        this.authToken = authToken;
    }

    private boolean isRegistryAuthorized(String authorization) {
        if (authToken == null || authToken.isBlank()) {
            return true; // Backward-compatible default.
        }

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring("Bearer ".length()).trim();
        return authToken.equals(token);
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
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody ValidateRequest request
    ) {
        if (!isRegistryAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody RegistryService.CreateServiceRequest request) {
        if (!isRegistryAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ServiceDTO created = registryService.createService(request);
        return ResponseEntity.created(URI.create("/api/v1/registry/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceDTO> updateService(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody RegistryService.UpdateServiceRequest request) {
        if (!isRegistryAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(registryService.updateService(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        if (!isRegistryAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
