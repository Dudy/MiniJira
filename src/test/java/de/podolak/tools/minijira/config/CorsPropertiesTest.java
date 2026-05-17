package de.podolak.tools.minijira.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CorsPropertiesTest {
    @Test
    void constructorNormalizesNullListsToEmptyLists() {
        CorsProperties properties = new CorsProperties(true, null, null, null, true);

        assertThat(properties.allowedOrigins()).isEmpty();
        assertThat(properties.allowedMethods()).isEmpty();
        assertThat(properties.allowedHeaders()).isEmpty();
    }

    @Test
    void constructorDefensivelyCopiesLists() {
        List<String> origins = new ArrayList<>(List.of("http://localhost:3000"));

        CorsProperties properties = new CorsProperties(true, origins, List.of("GET"), List.of("*"), true);
        origins.add("http://localhost:4000");

        assertThat(properties.allowedOrigins()).containsExactly("http://localhost:3000");
        assertThatThrownBy(() -> properties.allowedOrigins().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
