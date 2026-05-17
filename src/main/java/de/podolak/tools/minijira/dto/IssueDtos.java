package de.podolak.tools.minijira.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class IssueDtos {
    private IssueDtos() {
    }

    public record CreateIssueRequest(
            @NotNull List<@NotNull Integer> workerUserIds,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 20_000) String description,
            @NotNull @Min(1) @Max(5) Integer priority,
            @NotNull @Min(1) @Max(5) Integer status
    ) {
    }

    public record UpdateIssueRequest(
            @NotNull List<@NotNull Integer> workerUserIds,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 20_000) String description,
            @NotNull @Min(1) @Max(5) Integer priority,
            @NotNull @Min(1) @Max(5) Integer status
    ) {
    }

    public record IssueListItemDto(
            Integer id,
            UserDtos.UserDto author,
            List<UserDtos.UserDto> workers,
            String title,
            Integer priority,
            Integer status
    ) {
    }

    public record IssueDetailDto(
            Integer id,
            UserDtos.UserDto author,
            List<UserDtos.UserDto> workers,
            String title,
            String description,
            Integer priority,
            Integer status
    ) {
    }
}
