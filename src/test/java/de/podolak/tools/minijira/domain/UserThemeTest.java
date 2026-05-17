package de.podolak.tools.minijira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserThemeTest {
    @Test
    void definesSupportedThemes() {
        assertThat(UserTheme.values()).containsExactly(UserTheme.LIGHT, UserTheme.DARK, UserTheme.RETRO);
    }
}
