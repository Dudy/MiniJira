package de.podolak.tools.minijira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IssueTest {
    @Test
    void constructorStoresAllFields() {
        AppUser author = new AppUser("author", "password");
        AppUser worker = new AppUser("worker", "password");
        Set<AppUser> workers = new LinkedHashSet<>(Set.of(worker));

        Issue issue = new Issue(author, workers, "Title", "Description", IssuePriority.HIGH, IssueStatus.DOING);

        assertThat(issue.getAuthor()).isSameAs(author);
        assertThat(issue.getWorkers()).containsExactly(worker);
        assertThat(issue.getTitle()).isEqualTo("Title");
        assertThat(issue.getDescription()).isEqualTo("Description");
        assertThat(issue.getPriority()).isEqualTo(IssuePriority.HIGH);
        assertThat(issue.getStatus()).isEqualTo(IssueStatus.DOING);
    }

    @Test
    void settersUpdateMutableFields() {
        Issue issue = new Issue(new AppUser("author", "password"), new LinkedHashSet<>(), "Old", "Old description",
                IssuePriority.LOW, IssueStatus.TODO);
        AppUser worker = new AppUser("worker", "password");

        issue.setWorkers(new LinkedHashSet<>(Set.of(worker)));
        issue.setTitle("New");
        issue.setDescription("New description");
        issue.setPriority(IssuePriority.VERY_HIGH);
        issue.setStatus(IssueStatus.DONE);

        assertThat(issue.getWorkers()).containsExactly(worker);
        assertThat(issue.getTitle()).isEqualTo("New");
        assertThat(issue.getDescription()).isEqualTo("New description");
        assertThat(issue.getPriority()).isEqualTo(IssuePriority.VERY_HIGH);
        assertThat(issue.getStatus()).isEqualTo(IssueStatus.DONE);
    }
}
