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
        // But exclude API endpoints
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward root to index.html
        registry.addViewController("/")
                .setViewName("forward:/index.html");

        // Forward common app routes to index.html for React Router
        registry.addViewController("/services/{id}")
                .setViewName("forward:/index.html");
        registry.addViewController("/services/{id}/spec")
                .setViewName("forward:/index.html");
        registry.addViewController("/audit")
                .setViewName("forward:/index.html");

        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}
