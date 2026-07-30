package com.specpulse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.auth.repository.UserRepository;
import com.specpulse.auth.security.JwtAuthenticationEntryPoint;
import com.specpulse.auth.security.JwtAuthenticationFilter;
import com.specpulse.auth.security.JwtAuthenticationService;
import com.specpulse.auth.security.JwtProperties;
import com.specpulse.auth.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.specpulse.auth.security.RbacPermissions;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    JwtService jwtService(JwtProperties props, ObjectMapper objectMapper) {
        return new JwtService(props, objectMapper);
    }

    @Bean
    JwtAuthenticationService jwtAuthenticationService(JwtService jwtService, UserRepository userRepository) {
        return new JwtAuthenticationService(jwtService, userRepository);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtAuthenticationService jwtAuthenticationService) {
        return new JwtAuthenticationFilter(jwtAuthenticationService);
    }

    @Bean
    JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint entryPoint,
            JwtProperties jwtProperties
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh.authenticationEntryPoint(entryPoint));

        if (!jwtProperties.isEnabled()) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().permitAll()
            );
            return http.build();
        }

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/settings/public").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/favicon.ico",
                                "/robots.txt",
                                "/manifest.json"
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Admin-only endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // RBAC-protected mutations
                        .requestMatchers(HttpMethod.POST, "/api/v1/registry/validate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/registry").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/registry/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/registry/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/groups").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/groups/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/groups/**").hasRole("ADMIN")
                        // NOTE: use * instead of ** in the middle; Spring's PathPatternParser
                        // rejects patterns like /groups/**/services as invalid.
                        .requestMatchers(HttpMethod.POST, "/api/v1/groups/*/services").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/groups/*/services/*").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/diffs/compare").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pull/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/v1/settings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/settings/**").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/tests/proxy").hasAuthority(RbacPermissions.API_TEST_EXECUTE_AUTHORITY)

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Other endpoints remain public (read-only endpoints)
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
