package de.podolak.tools.minijira.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebConfig implements WebMvcConfigurer {
    private final CorsProperties corsProperties;

    public WebConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!corsProperties.enabled()) {
            return;
        }

        var registration = registry.addMapping("/**")
                .allowedMethods(toArray(corsProperties.allowedMethods()))
                .allowedHeaders(toArray(corsProperties.allowedHeaders()))
                .allowCredentials(corsProperties.allowCredentials());

        if (!corsProperties.allowedOrigins().isEmpty()) {
            registration.allowedOrigins(toArray(corsProperties.allowedOrigins()));
        }
    }

    private static String[] toArray(List<String> values) {
        return values.toArray(String[]::new);
    }
}
