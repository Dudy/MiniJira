package de.podolak.tools.minijira.api;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.Issue;
import de.podolak.tools.minijira.dto.IssueDtos.IssueDetailDto;
import de.podolak.tools.minijira.dto.IssueDtos.IssueListItemDto;
import de.podolak.tools.minijira.dto.UserDtos.UserDto;
import java.util.List;

final class DtoMapper {
    private DtoMapper() {
    }

    static UserDto toUserDto(AppUser user) {
        return new UserDto(user.getId(), user.getUsername());
    }

    static IssueListItemDto toIssueListItemDto(Issue issue) {
        return new IssueListItemDto(
                issue.getId(),
                toUserDto(issue.getAuthor()),
                issue.getWorkers().stream().map(DtoMapper::toUserDto).toList(),
                issue.getTitle(),
                issue.getPriority(),
                issue.getStatus()
        );
    }

    static IssueDetailDto toIssueDetailDto(Issue issue) {
        List<UserDto> workers = issue.getWorkers().stream().map(DtoMapper::toUserDto).toList();
        return new IssueDetailDto(
                issue.getId(),
                toUserDto(issue.getAuthor()),
                workers,
                issue.getTitle(),
                issue.getDescription(),
                issue.getPriority(),
                issue.getStatus()
        );
    }
}
