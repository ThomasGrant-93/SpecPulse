package com.specpulse.api;

import com.specpulse.history.AuditEventType;
import com.specpulse.history.AuditLogDTO;
import com.specpulse.history.domain.AuditReadPort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditReadPort auditService;

    public AuditController(AuditReadPort auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogsByService(@PathVariable Long serviceId) {
        return ResponseEntity.ok(auditService.getAuditLogsByServiceId(serviceId));
    }

    @GetMapping("/event-type/{eventType}")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogsByEventType(@PathVariable String eventType) {
        AuditEventType type = AuditEventType.valueOf(eventType);
        return ResponseEntity.ok(auditService.getAuditLogsByEventType(type));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AuditLogDTO>> getRecentAuditLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditService.getRecentAuditLogs(limit));
    }

    @GetMapping("/since")
    public ResponseEntity<List<AuditLogDTO>> getAuditLogsSince(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return ResponseEntity.ok(auditService.getAuditLogsSince(since));
    }
}
