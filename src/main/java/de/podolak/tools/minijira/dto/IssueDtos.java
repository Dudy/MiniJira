package de.podolak.tools.minijira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class IssueDtos {
    private IssueDtos() {
    }

    public record CreateIssueRequest(
            @NotNull List<@NotNull Integer> workerUserIds,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 20_000) String description,
            @NotBlank @Pattern(regexp = "^(VERY_HIGH|HIGH|MEDIUM|LOW|VERY_LOW)$") String priority,
            @NotBlank @Pattern(regexp = "^(TODO|DOING|TESTING|REVIEWING|DONE)$") String status
    ) {
    }

    public record UpdateIssueRequest(
            @NotNull List<@NotNull Integer> workerUserIds,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 20_000) String description,
            @NotBlank @Pattern(regexp = "^(VERY_HIGH|HIGH|MEDIUM|LOW|VERY_LOW)$") String priority,
            @NotBlank @Pattern(regexp = "^(TODO|DOING|TESTING|REVIEWING|DONE)$") String status
    ) {
    }

    public record IssueListItemDto(
            Integer id,
            UserDtos.UserDto author,
            List<UserDtos.UserDto> workers,
            String title,
            String priority,
            String status
    ) {
    }

    public record IssueDetailDto(
            Integer id,
            UserDtos.UserDto author,
            List<UserDtos.UserDto> workers,
            String title,
            String description,
            String priority,
            String status
    ) {
    }
}
