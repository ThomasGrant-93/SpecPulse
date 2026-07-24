package com.specpulse.api.mapper;

import com.specpulse.api.RegistryController;
import com.specpulse.registry.ServiceValidationRequest;
import com.specpulse.registry.ServiceValidationResult;
import org.springframework.stereotype.Component;

@Component
public class RegistryApiMapper {

    public ServiceValidationRequest toServiceValidationRequest(RegistryController.ValidateRequest request) {
        return new ServiceValidationRequest(request.name(), request.openApiUrl());
    }

    public RegistryController.ValidateResponse toValidateResponse(ServiceValidationResult result) {
        return new RegistryController.ValidateResponse(result.valid(), result.errors());
    }
}
