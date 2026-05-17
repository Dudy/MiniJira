package de.podolak.tools.minijira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 80) String username,
            @NotBlank @Size(min = 1, max = 255) String password
    ) {
    }

    public record UserDto(Integer id, String username) {
    }
}
