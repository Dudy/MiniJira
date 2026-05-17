package de.podolak.tools.minijira.api;

import de.podolak.tools.minijira.domain.Issue;
import de.podolak.tools.minijira.dto.IssueDtos.CreateIssueRequest;
import de.podolak.tools.minijira.dto.IssueDtos.IssueDetailDto;
import de.podolak.tools.minijira.dto.IssueDtos.IssueListItemDto;
import de.podolak.tools.minijira.dto.IssueDtos.UpdateIssueRequest;
import de.podolak.tools.minijira.service.CreateIssueCommand;
import de.podolak.tools.minijira.service.IssueService;
import de.podolak.tools.minijira.service.IssueSortField;
import de.podolak.tools.minijira.service.UpdateIssueCommand;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/issues")
public class IssueController {
    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    public ResponseEntity<IssueDetailDto> createIssue(@Valid @RequestBody CreateIssueRequest request) {
        CreateIssueCommand command = new CreateIssueCommand(
                request.authorUserId(),
                request.workerUserIds(),
                request.title(),
                request.description(),
                request.priority()
        );
        Issue issue = issueService.createIssue(command);
        return ResponseEntity.created(URI.create("/api/issues/" + issue.getId()))
                .body(DtoMapper.toIssueDetailDto(issue));
    }

    @GetMapping
    public List<IssueListItemDto> listIssues(
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        IssueSortField sortField = IssueSortField.fromRequestValue(sort);
        return issueService.listIssues(sortField, sortDirection).stream()
                .map(DtoMapper::toIssueListItemDto)
                .toList();
    }

    @GetMapping("/{id}")
    public IssueDetailDto getIssue(@PathVariable Integer id) {
        return DtoMapper.toIssueDetailDto(issueService.getIssue(id));
    }

    @PutMapping("/{id}")
    public IssueDetailDto updateIssue(@PathVariable Integer id, @Valid @RequestBody UpdateIssueRequest request) {
        UpdateIssueCommand command = new UpdateIssueCommand(
                request.workerUserIds(),
                request.title(),
                request.description()
        );
        return DtoMapper.toIssueDetailDto(issueService.updateIssue(id, command));
    }
}
