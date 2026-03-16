package com.specpulse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:SpecPulse}")
    private String appName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " API")
                        .version("0.1.0")
                        .description("API для управления OpenAPI спецификациями: регистрация сервисов, pull спецификаций, сравнение версий, детектирование breaking changes")
                        .contact(new Contact()
                                .name("SpecPulse Team")
                                .email("support@specpulse.com")));
    }
}
