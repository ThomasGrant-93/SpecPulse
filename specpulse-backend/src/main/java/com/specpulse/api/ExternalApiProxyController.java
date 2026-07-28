package com.specpulse.api;

import com.specpulse.proxy.ExternalApiProxyRequest;
import com.specpulse.proxy.ExternalApiProxyResponse;
import com.specpulse.proxy.ExternalApiProxyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tests")
public class ExternalApiProxyController {

    private final ExternalApiProxyService proxyService;

    // If configured (non-empty), requires Authorization: Bearer <token> for proxying.
    private final String authToken;

    public ExternalApiProxyController(
            ExternalApiProxyService proxyService,
            @Value("${specpulse.auth.token:}") String authToken
    ) {
        this.proxyService = proxyService;
        this.authToken = authToken;
    }

    @PostMapping("/proxy")
    public ResponseEntity<ExternalApiProxyResponse> proxy(
            @RequestBody ExternalApiProxyRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        if (authToken != null && !authToken.isBlank()) {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .build();
            }

            String token = authorization.substring("Bearer ".length()).trim();
            if (!authToken.equals(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .build();
            }
        }

        ExternalApiProxyResponse response = proxyService.proxy(request);

        // When proxying fails we return status=0 from the service; expose this as a meaningful HTTP status
        // for observability/alerting while keeping the response body shape stable.
        if (response.status() == 0) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
