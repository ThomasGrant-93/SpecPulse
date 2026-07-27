package com.specpulse.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.specpulse.proxy.ExternalApiProxyRequest;
import com.specpulse.proxy.ExternalApiProxyResponse;
import com.specpulse.proxy.ExternalApiProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tests")
public class ExternalApiProxyController {

    private final ExternalApiProxyService proxyService;

    public ExternalApiProxyController(ExternalApiProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @PostMapping("/proxy")
    public ResponseEntity<ExternalApiProxyResponse> proxy(@RequestBody ExternalApiProxyRequest request) {
        return ResponseEntity.ok(proxyService.proxy(request));
    }
}
