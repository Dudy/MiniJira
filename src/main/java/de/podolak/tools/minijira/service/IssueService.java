package de.podolak.tools.minijira.service;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.Issue;
import de.podolak.tools.minijira.domain.IssuePriority;
import de.podolak.tools.minijira.domain.IssueStatus;
import de.podolak.tools.minijira.repo.IssueRepository;
import de.podolak.tools.minijira.repo.UserRepository;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    public IssueService(IssueRepository issueRepository, UserRepository userRepository) {
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Issue createIssue(CreateIssueCommand command) {
        AppUser author = findUser(command.authorUserId());
        Set<AppUser> workers = findUsers(command.workerUserIds());

        Issue issue = new Issue(
                author,
                workers,
                command.title().trim(),
                command.description().trim(),
                findPriority(command.priority()),
                findStatus(command.status())
        );
        return issueRepository.save(issue);
    }

    @Transactional(readOnly = true)
    public List<Issue> listIssues(IssueSortField sortField, Sort.Direction direction) {
        Comparator<Issue> comparator = switch (sortField) {
            case ID -> Comparator.comparing(Issue::getId);
            case AUTHOR -> Comparator.comparing(
                    (Issue issue) -> issue.getAuthor() == null ? "" : issue.getAuthor().getUsername(),
                    String.CASE_INSENSITIVE_ORDER
            );
            case PRIORITY -> Comparator.comparingInt(
                    (Issue issue) -> issue.getPriority() == null ? Integer.MAX_VALUE : issue.getPriority().ordinal()
            );
        };
        if (direction == Sort.Direction.DESC) {
            comparator = comparator.reversed();
        }
        return issueRepository.findAll().stream()
                .sorted(comparator.thenComparing(Issue::getId))
                .toList();
    }

    @Transactional(readOnly = true)
    public Issue getIssue(Integer id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Issue not found: " + id));
    }

    @Transactional
    public Issue updateIssue(Integer id, UpdateIssueCommand command) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Issue not found: " + id));

        issue.setWorkers(findUsers(command.workerUserIds()));
        issue.setTitle(command.title().trim());
        issue.setDescription(command.description().trim());
        issue.setPriority(findPriority(command.priority()));
        issue.setStatus(findStatus(command.status()));
        return issue;
    }

    private AppUser findUser(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Unknown user id: " + id));
    }

    private Set<AppUser> findUsers(List<Integer> ids) {
        if (ids == null) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<AppUser> result = new LinkedHashSet<>();
        for (Integer id : ids) {
            result.add(findUser(id));
        }
        return result;
    }

    private IssuePriority findPriority(String value) {
        try {
            return IssuePriority.fromRequestValue(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private IssueStatus findStatus(String value) {
        try {
            return IssueStatus.fromRequestValue(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
