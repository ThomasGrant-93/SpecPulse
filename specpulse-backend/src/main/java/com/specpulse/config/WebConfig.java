package com.specpulse.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static files from classpath:/static/
        // IMPORTANT: do not register a catch-all `/**` handler.
        // It can intercept springdoc Swagger UI resource paths (e.g. `/swagger-ui/`),
        // causing them to be treated as missing static resources.

        // Vite puts hashed assets into dist/assets/*.
        // With pattern `/assets/**`, Spring strips the `/assets/` prefix when resolving resources,
        // so the resource location must point to `classpath:/static/assets/`.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(3600)
                .resourceChain(true);

        // Root static files (non-hashed)
        registry.addResourceHandler(
                        "/index.html",
                        "/favicon.ico",
                        "/robots.txt",
                        "/manifest.json")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Compatibility with common Swagger UI entrypoints.
        // springdoc primarily serves `/swagger-ui/index.html` (and `/swagger-ui` redirects to it),
        // but browsers/users often request variants like `/swagger-ui/` or `/swagger-ui.html`.
        registry.addViewController("/swagger-ui/")
                .setViewName("forward:/swagger-ui/index.html");

        registry.addViewController("/swagger-ui.html")
                .setViewName("forward:/swagger-ui/index.html");

        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
