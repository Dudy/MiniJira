package de.podolak.tools.minijira.service;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.Issue;
import de.podolak.tools.minijira.repo.IssueRepository;
import de.podolak.tools.minijira.repo.UserRepository;
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
                command.priority()
        );
        return issueRepository.save(issue);
    }

    @Transactional(readOnly = true)
    public List<Issue> listIssues(IssueSortField sortField, Sort.Direction direction) {
        Sort sort = switch (sortField) {
            case ID -> Sort.by(direction, "id");
            case AUTHOR -> Sort.by(direction, "author.id").and(Sort.by(Sort.Direction.ASC, "id"));
            case PRIORITY -> Sort.by(direction, "priority").and(Sort.by(Sort.Direction.ASC, "id"));
        };
        return issueRepository.findAll(sort);
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
        issue.setPriority(command.priority());
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
}
