package de.podolak.tools.minijira.api;

import static org.assertj.core.api.Assertions.assertThat;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.Issue;
import de.podolak.tools.minijira.domain.IssuePriority;
import de.podolak.tools.minijira.domain.IssueStatus;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DtoMapperTest {
    @Test
    void toUserDtoMapsIdAndUsername() {
        AppUser user = userWithId(1, "alice");

        var dto = DtoMapper.toUserDto(user);

        assertThat(dto.id()).isEqualTo(1);
        assertThat(dto.username()).isEqualTo("alice");
    }

    @Test
    void toIssueListItemDtoMapsSummaryFieldsAndWorkers() {
        Issue issue = issueWithId(10);

        var dto = DtoMapper.toIssueListItemDto(issue);

        assertThat(dto.id()).isEqualTo(10);
        assertThat(dto.author().username()).isEqualTo("author");
        assertThat(dto.workers()).extracting("username").containsExactly("worker-a", "worker-b");
        assertThat(dto.title()).isEqualTo("Title");
        assertThat(dto.priority()).isEqualTo("HIGH");
        assertThat(dto.status()).isEqualTo("DOING");
    }

    @Test
    void toIssueDetailDtoMapsDescriptionInAdditionToSummaryFields() {
        Issue issue = issueWithId(10);

        var dto = DtoMapper.toIssueDetailDto(issue);

        assertThat(dto.id()).isEqualTo(10);
        assertThat(dto.author().username()).isEqualTo("author");
        assertThat(dto.workers()).extracting("username").containsExactly("worker-a", "worker-b");
        assertThat(dto.title()).isEqualTo("Title");
        assertThat(dto.description()).isEqualTo("Description");
        assertThat(dto.priority()).isEqualTo("HIGH");
        assertThat(dto.status()).isEqualTo("DOING");
    }

    private static Issue issueWithId(Integer id) {
        AppUser author = userWithId(1, "author");
        AppUser workerA = userWithId(2, "worker-a");
        AppUser workerB = userWithId(3, "worker-b");
        Issue issue = new Issue(author, new LinkedHashSet<>(List.of(workerA, workerB)), "Title", "Description",
                IssuePriority.HIGH, IssueStatus.DOING);
        ReflectionTestUtils.setField(issue, "id", id);
        return issue;
    }

    private static AppUser userWithId(Integer id, String username) {
        AppUser user = new AppUser(username, "secret");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
