package de.podolak.tools.minijira.config;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebConfigTest {
    @Test
    void addCorsMappingsDoesNothingWhenCorsIsDisabled() {
        CorsRegistry registry = org.mockito.Mockito.mock(CorsRegistry.class);
        WebConfig config = new WebConfig(new CorsProperties(false, List.of("*"), List.of("GET"), List.of("*"),
                true));

        config.addCorsMappings(registry);

        verify(registry, never()).addMapping(org.mockito.Mockito.anyString());
    }

    @Test
    void addCorsMappingsRegistersConfiguredValuesWhenEnabled() {
        CorsRegistry registry = org.mockito.Mockito.mock(CorsRegistry.class);
        CorsRegistration registration = org.mockito.Mockito.mock(CorsRegistration.class);
        when(registry.addMapping("/**")).thenReturn(registration);
        when(registration.allowedMethods("GET", "POST")).thenReturn(registration);
        when(registration.allowedHeaders("Content-Type", "X-Test")).thenReturn(registration);
        when(registration.allowCredentials(true)).thenReturn(registration);
        WebConfig config = new WebConfig(new CorsProperties(true, List.of("http://localhost:3000"),
                List.of("GET", "POST"), List.of("Content-Type", "X-Test"), true));

        config.addCorsMappings(registry);

        verify(registry).addMapping("/**");
        verify(registration).allowedMethods("GET", "POST");
        verify(registration).allowedHeaders("Content-Type", "X-Test");
        verify(registration).allowCredentials(true);
        verify(registration).allowedOrigins("http://localhost:3000");
    }

    @Test
    void addCorsMappingsSkipsOriginsWhenNoneAreConfigured() {
        CorsRegistry registry = org.mockito.Mockito.mock(CorsRegistry.class);
        CorsRegistration registration = org.mockito.Mockito.mock(CorsRegistration.class);
        when(registry.addMapping("/**")).thenReturn(registration);
        when(registration.allowedMethods()).thenReturn(registration);
        when(registration.allowedHeaders()).thenReturn(registration);
        when(registration.allowCredentials(false)).thenReturn(registration);
        WebConfig config = new WebConfig(new CorsProperties(true, null, null, null, false));

        config.addCorsMappings(registry);

        verify(registration, never()).allowedOrigins(org.mockito.Mockito.any());
    }
}
