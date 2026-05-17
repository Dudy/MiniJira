package de.podolak.tools.minijira.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IssueStatusTest {
    @Test
    void labelReturnsConfiguredDisplayText() {
        assertThat(IssueStatus.TODO.label()).isEqualTo("to do");
        assertThat(IssueStatus.REVIEWING.label()).isEqualTo("reviewing");
    }

    @Test
    void fromRequestValueAcceptsTrimmedCaseInsensitiveNames() {
        assertThat(IssueStatus.fromRequestValue(" done ")).isEqualTo(IssueStatus.DONE);
        assertThat(IssueStatus.fromRequestValue("TESTING")).isEqualTo(IssueStatus.TESTING);
    }

    @Test
    void fromRequestValueRejectsNullAndUnknownValues() {
        assertThatThrownBy(() -> IssueStatus.fromRequestValue(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Status is required");
        assertThatThrownBy(() -> IssueStatus.fromRequestValue("blocked"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported issue status: blocked");
    }
}
