package de.podolak.tools.minijira.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IssuePriorityTest {
    @Test
    void labelReturnsConfiguredDisplayText() {
        assertThat(IssuePriority.VERY_HIGH.label()).isEqualTo("Sehr hoch");
        assertThat(IssuePriority.VERY_LOW.label()).isEqualTo("Sehr niedrig");
    }

    @Test
    void fromRequestValueAcceptsTrimmedCaseInsensitiveNames() {
        assertThat(IssuePriority.fromRequestValue(" high ")).isEqualTo(IssuePriority.HIGH);
        assertThat(IssuePriority.fromRequestValue("VERY_LOW")).isEqualTo(IssuePriority.VERY_LOW);
    }

    @Test
    void fromRequestValueRejectsNullAndUnknownValues() {
        assertThatThrownBy(() -> IssuePriority.fromRequestValue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Priority is required");
        assertThatThrownBy(() -> IssuePriority.fromRequestValue("urgent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported issue priority: urgent");
    }
}
