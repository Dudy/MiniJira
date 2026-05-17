package de.podolak.tools.minijira.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.Issue;
import de.podolak.tools.minijira.domain.IssuePriority;
import de.podolak.tools.minijira.domain.IssueStatus;
import de.podolak.tools.minijira.dto.IssueDtos.CreateIssueRequest;
import de.podolak.tools.minijira.dto.IssueDtos.UpdateIssueRequest;
import de.podolak.tools.minijira.service.AuthenticationException;
import de.podolak.tools.minijira.service.CreateIssueCommand;
import de.podolak.tools.minijira.service.IssueService;
import de.podolak.tools.minijira.service.IssueSortField;
import de.podolak.tools.minijira.service.UpdateIssueCommand;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IssueControllerTest {
    @Mock
    private IssueService issueService;

    @Mock
    private HttpSession session;

    @Test
    void createIssueUsesCurrentSessionUserAndReturnsCreatedResponse() {
        IssueController controller = new IssueController(issueService);
        Issue issue = issueWithId(99, "Title");
        when(session.getAttribute(SessionController.SESSION_USER_ID)).thenReturn(7);
        when(issueService.createIssue(any(CreateIssueCommand.class))).thenReturn(issue);

        var response = controller.createIssue(new CreateIssueRequest(List.of(2, 3), "Title", "Description",
                "HIGH", "TODO"), session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation().toString()).isEqualTo("/api/issues/99");
        assertThat(response.getBody().id()).isEqualTo(99);
        ArgumentCaptor<CreateIssueCommand> commandCaptor = ArgumentCaptor.forClass(CreateIssueCommand.class);
        verify(issueService).createIssue(commandCaptor.capture());
        assertThat(commandCaptor.getValue().authorUserId()).isEqualTo(7);
        assertThat(commandCaptor.getValue().workerUserIds()).containsExactly(2, 3);
        assertThat(commandCaptor.getValue().priority()).isEqualTo("HIGH");
    }

    @Test
    void createIssueRejectsMissingSessionUser() {
        IssueController controller = new IssueController(issueService);
        when(session.getAttribute(SessionController.SESSION_USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> controller.createIssue(new CreateIssueRequest(List.of(), "Title", "Description",
                "LOW", "TODO"), session))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Not logged in");
    }

    @Test
    void listIssuesParsesSortParametersAndMapsResponse() {
        IssueController controller = new IssueController(issueService);
        when(issueService.listIssues(IssueSortField.AUTHOR, Sort.Direction.DESC))
                .thenReturn(List.of(issueWithId(2, "Second"), issueWithId(1, "First")));

        var result = controller.listIssues("author", "desc");

        assertThat(result).extracting("id").containsExactly(2, 1);
        verify(issueService).listIssues(IssueSortField.AUTHOR, Sort.Direction.DESC);
    }

    @Test
    void getIssueDelegatesAndMapsDetailDto() {
        IssueController controller = new IssueController(issueService);
        when(issueService.getIssue(5)).thenReturn(issueWithId(5, "Title"));

        var dto = controller.getIssue(5);

        assertThat(dto.id()).isEqualTo(5);
        assertThat(dto.title()).isEqualTo("Title");
        verify(issueService).getIssue(5);
    }

    @Test
    void updateIssueBuildsCommandAndMapsDetailDto() {
        IssueController controller = new IssueController(issueService);
        when(issueService.updateIssue(any(Integer.class), any(UpdateIssueCommand.class)))
                .thenReturn(issueWithId(5, "Updated"));

        var dto = controller.updateIssue(5, new UpdateIssueRequest(List.of(2), "Updated", "Description",
                "VERY_HIGH", "DONE"));

        assertThat(dto.id()).isEqualTo(5);
        assertThat(dto.title()).isEqualTo("Updated");
        ArgumentCaptor<UpdateIssueCommand> commandCaptor = ArgumentCaptor.forClass(UpdateIssueCommand.class);
        verify(issueService).updateIssue(org.mockito.Mockito.eq(5), commandCaptor.capture());
        assertThat(commandCaptor.getValue().workerUserIds()).containsExactly(2);
        assertThat(commandCaptor.getValue().priority()).isEqualTo("VERY_HIGH");
        assertThat(commandCaptor.getValue().status()).isEqualTo("DONE");
    }

    private static Issue issueWithId(Integer id, String title) {
        AppUser author = new AppUser("author", "secret");
        Issue issue = new Issue(author, new LinkedHashSet<>(), title, "Description", IssuePriority.HIGH,
                IssueStatus.DOING);
        ReflectionTestUtils.setField(issue, "id", id);
        ReflectionTestUtils.setField(author, "id", 1);
        return issue;
    }
}
