package de.podolak.tools.minijira;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class MiniJiraApplicationTest {
    @Test
    void applicationClassIsSpringBootApplication() {
        assertThat(MiniJiraApplication.class).hasAnnotation(SpringBootApplication.class);
    }
}
