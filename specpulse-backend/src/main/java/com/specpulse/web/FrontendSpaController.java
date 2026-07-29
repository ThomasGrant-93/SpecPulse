package com.specpulse.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FrontendSpaController {

    private final Resource indexHtml = new ClassPathResource("static/index.html");

    @GetMapping({
            "/",
            "/services/{id}",
            "/services/{id}/spec",
            "/services/{id}/diffs",
            "/groups",
            "/settings",
            "/audit"
    })
    public ResponseEntity<Resource> index(@PathVariable(name = "id", required = false) String id) {
        // Serve the already-built Vite React application.
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
    }
}
