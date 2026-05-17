package de.podolak.tools.minijira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IssueSortFieldTest {
    @Test
    void fromRequestValueDefaultsBlankAndNullToId() {
        assertThat(IssueSortField.fromRequestValue(null)).isEqualTo(IssueSortField.ID);
        assertThat(IssueSortField.fromRequestValue(" ")).isEqualTo(IssueSortField.ID);
    }

    @Test
    void fromRequestValueAcceptsSupportedAliases() {
        assertThat(IssueSortField.fromRequestValue("id")).isEqualTo(IssueSortField.ID);
        assertThat(IssueSortField.fromRequestValue(" author ")).isEqualTo(IssueSortField.AUTHOR);
        assertThat(IssueSortField.fromRequestValue("autor")).isEqualTo(IssueSortField.AUTHOR);
        assertThat(IssueSortField.fromRequestValue("priority")).isEqualTo(IssueSortField.PRIORITY);
        assertThat(IssueSortField.fromRequestValue("prioritaet")).isEqualTo(IssueSortField.PRIORITY);
    }

    @Test
    void fromRequestValueRejectsUnsupportedValue() {
        assertThatThrownBy(() -> IssueSortField.fromRequestValue("status"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported issue sort field: status");
    }
}
