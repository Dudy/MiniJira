package de.podolak.tools.minijira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.Issue;
import de.podolak.tools.minijira.domain.IssuePriority;
import de.podolak.tools.minijira.domain.IssueStatus;
import de.podolak.tools.minijira.repo.IssueRepository;
import de.podolak.tools.minijira.repo.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {
    @Mock
    private IssueRepository issueRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void createIssueBuildsTrimmedIssueAndSavesIt() {
        IssueService service = new IssueService(issueRepository, userRepository);
        AppUser author = userWithId(1, "author");
        AppUser worker = userWithId(2, "worker");
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(userRepository.findById(2)).thenReturn(Optional.of(worker));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue created = service.createIssue(new CreateIssueCommand(1, List.of(2), " Title ", " Description ",
                "high", "doing"));

        assertThat(created.getAuthor()).isSameAs(author);
        assertThat(created.getWorkers()).containsExactly(worker);
        assertThat(created.getTitle()).isEqualTo("Title");
        assertThat(created.getDescription()).isEqualTo("Description");
        assertThat(created.getPriority()).isEqualTo(IssuePriority.HIGH);
        assertThat(created.getStatus()).isEqualTo(IssueStatus.DOING);
        verify(issueRepository).save(created);
    }

    @Test
    void createIssueAllowsNullWorkersAsEmptySetAndDeduplicatesWorkerIds() {
        IssueService service = new IssueService(issueRepository, userRepository);
        AppUser author = userWithId(1, "author");
        AppUser worker = userWithId(2, "worker");
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(userRepository.findById(2)).thenReturn(Optional.of(worker));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Issue withoutWorkers = service.createIssue(new CreateIssueCommand(1, null, "Title", "Description",
                "LOW", "TODO"));
        Issue deduped = service.createIssue(new CreateIssueCommand(1, List.of(2, 2), "Title", "Description",
                "LOW", "TODO"));

        assertThat(withoutWorkers.getWorkers()).isEmpty();
        assertThat(deduped.getWorkers()).containsExactly(worker);
    }

    @Test
    void createIssueRejectsUnknownUsersAndInvalidEnums() {
        IssueService service = new IssueService(issueRepository, userRepository);
        AppUser author = userWithId(1, "author");
        when(userRepository.findById(404)).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createIssue(new CreateIssueCommand(404, List.of(), "Title",
                "Description", "LOW", "TODO")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unknown user id: 404");
        assertThatThrownBy(() -> service.createIssue(new CreateIssueCommand(1, List.of(2), "Title",
                "Description", "LOW", "TODO")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unknown user id: 2");
        assertThatThrownBy(() -> service.createIssue(new CreateIssueCommand(1, List.of(), "Title",
                "Description", "urgent", "TODO")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported issue priority: urgent");
        assertThatThrownBy(() -> service.createIssue(new CreateIssueCommand(1, List.of(), "Title",
                "Description", "LOW", "blocked")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported issue status: blocked");
        verify(issueRepository, never()).save(any());
    }

    @Test
    void listIssuesSortsByIdAscendingAndDescending() {
        IssueService service = new IssueService(issueRepository, userRepository);
        Issue issue2 = issueWithId(2, "bob", IssuePriority.LOW);
        Issue issue1 = issueWithId(1, "alice", IssuePriority.HIGH);
        when(issueRepository.findAll()).thenReturn(List.of(issue2, issue1));

        assertThat(service.listIssues(IssueSortField.ID, Sort.Direction.ASC)).extracting(Issue::getId)
                .containsExactly(1, 2);
        assertThat(service.listIssues(IssueSortField.ID, Sort.Direction.DESC)).extracting(Issue::getId)
                .containsExactly(2, 1);
    }

    @Test
    void listIssuesSortsByAuthorCaseInsensitiveAndUsesIdTieBreaker() {
        IssueService service = new IssueService(issueRepository, userRepository);
        Issue noAuthor = issueWithId(4, null, IssuePriority.LOW);
        Issue beta = issueWithId(3, "Beta", IssuePriority.LOW);
        Issue alpha2 = issueWithId(2, "alpha", IssuePriority.LOW);
        Issue alpha1 = issueWithId(1, "Alpha", IssuePriority.LOW);
        when(issueRepository.findAll()).thenReturn(List.of(beta, alpha2, noAuthor, alpha1));

        assertThat(service.listIssues(IssueSortField.AUTHOR, Sort.Direction.ASC)).extracting(Issue::getId)
                .containsExactly(4, 1, 2, 3);
    }

    @Test
    void listIssuesSortsByPriorityOrdinalAndNullsLast() {
        IssueService service = new IssueService(issueRepository, userRepository);
        Issue low = issueWithId(3, "low", IssuePriority.LOW);
        Issue high = issueWithId(2, "high", IssuePriority.HIGH);
        Issue nullPriority = issueWithId(1, "null", null);
        when(issueRepository.findAll()).thenReturn(List.of(low, nullPriority, high));

        assertThat(service.listIssues(IssueSortField.PRIORITY, Sort.Direction.ASC)).extracting(Issue::getId)
                .containsExactly(2, 3, 1);
    }

    @Test
    void getIssueReturnsIssueOrThrowsNotFound() {
        IssueService service = new IssueService(issueRepository, userRepository);
        Issue issue = issueWithId(7, "author", IssuePriority.MEDIUM);
        when(issueRepository.findById(7)).thenReturn(Optional.of(issue));
        when(issueRepository.findById(8)).thenReturn(Optional.empty());

        assertThat(service.getIssue(7)).isSameAs(issue);
        assertThatThrownBy(() -> service.getIssue(8))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Issue not found: 8");
    }

    @Test
    void updateIssueMutatesExistingIssue() {
        IssueService service = new IssueService(issueRepository, userRepository);
        Issue issue = issueWithId(7, "author", IssuePriority.LOW);
        AppUser worker = userWithId(2, "worker");
        when(issueRepository.findById(7)).thenReturn(Optional.of(issue));
        when(userRepository.findById(2)).thenReturn(Optional.of(worker));

        Issue updated = service.updateIssue(7, new UpdateIssueCommand(List.of(2), " New ", " Description ",
                "VERY_HIGH", "DONE"));

        assertThat(updated).isSameAs(issue);
        assertThat(issue.getWorkers()).containsExactly(worker);
        assertThat(issue.getTitle()).isEqualTo("New");
        assertThat(issue.getDescription()).isEqualTo("Description");
        assertThat(issue.getPriority()).isEqualTo(IssuePriority.VERY_HIGH);
        assertThat(issue.getStatus()).isEqualTo(IssueStatus.DONE);
    }

    @Test
    void updateIssueRejectsMissingIssueBeforeLookingUpWorkers() {
        IssueService service = new IssueService(issueRepository, userRepository);
        when(issueRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateIssue(404, new UpdateIssueCommand(List.of(2), "Title",
                "Description", "LOW", "TODO")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Issue not found: 404");
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updateIssueRejectsUnknownWorkerAndInvalidEnums() {
        IssueService service = new IssueService(issueRepository, userRepository);
        Issue issue = issueWithId(7, "author", IssuePriority.LOW);
        when(issueRepository.findById(7)).thenReturn(Optional.of(issue));
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateIssue(7, new UpdateIssueCommand(List.of(2), "Title",
                "Description", "LOW", "TODO")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unknown user id: 2");
        assertThatThrownBy(() -> service.updateIssue(7, new UpdateIssueCommand(List.of(), "Title",
                "Description", null, "TODO")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Priority is required");
        assertThatThrownBy(() -> service.updateIssue(7, new UpdateIssueCommand(List.of(), "Title",
                "Description", "LOW", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Status is required");
    }

    @Test
    void updateIssueDoesNotSaveBecauseEntityIsMutatedInTransaction() {
        IssueService service = new IssueService(issueRepository, userRepository);
        Issue issue = issueWithId(7, "author", IssuePriority.LOW);
        when(issueRepository.findById(7)).thenReturn(Optional.of(issue));

        service.updateIssue(7, new UpdateIssueCommand(List.of(), "Title", "Description", "LOW", "TODO"));

        verify(issueRepository, never()).save(any());
    }

    @Test
    void createIssuePassesCreatedIssueToRepository() {
        IssueService service = new IssueService(issueRepository, userRepository);
        AppUser author = userWithId(1, "author");
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(issueRepository.save(any(Issue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Issue> issueCaptor = ArgumentCaptor.forClass(Issue.class);

        service.createIssue(new CreateIssueCommand(1, List.of(), "Title", "Description", "MEDIUM", "TESTING"));

        verify(issueRepository).save(issueCaptor.capture());
        assertThat(issueCaptor.getValue().getPriority()).isEqualTo(IssuePriority.MEDIUM);
        assertThat(issueCaptor.getValue().getStatus()).isEqualTo(IssueStatus.TESTING);
    }

    private static AppUser userWithId(Integer id, String username) {
        AppUser user = new AppUser(username, "secret");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Issue issueWithId(Integer id, String authorUsername, IssuePriority priority) {
        AppUser author = authorUsername == null ? null : new AppUser(authorUsername, "secret");
        Issue issue = new Issue(author, new LinkedHashSet<>(), "Issue " + id, "Description", priority,
                IssueStatus.TODO);
        ReflectionTestUtils.setField(issue, "id", id);
        return issue;
    }
}
