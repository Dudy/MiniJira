package de.podolak.tools.minijira.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        boolean enabled,
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        boolean allowCredentials
) {
    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        allowedMethods = allowedMethods == null ? List.of() : List.copyOf(allowedMethods);
        allowedHeaders = allowedHeaders == null ? List.of() : List.copyOf(allowedHeaders);
    }
}
