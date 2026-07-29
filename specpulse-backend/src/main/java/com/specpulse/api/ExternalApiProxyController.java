package com.specpulse.api;

import com.specpulse.proxy.ExternalApiProxyRequest;
import com.specpulse.proxy.ExternalApiProxyResponse;
import com.specpulse.proxy.ExternalApiProxyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tests")
public class ExternalApiProxyController {

    private final ExternalApiProxyService proxyService;

    public ExternalApiProxyController(
            ExternalApiProxyService proxyService
    ) {
        this.proxyService = proxyService;
    }

    @PostMapping("/proxy")
    public ResponseEntity<ExternalApiProxyResponse> proxy(
            @RequestBody ExternalApiProxyRequest request
    ) {
        ExternalApiProxyResponse response = proxyService.proxy(request);

        // When proxying fails we return status=0 from the service; expose this as a meaningful HTTP status
        // for observability/alerting while keeping the response body shape stable.
        if (response.status() == 0) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
