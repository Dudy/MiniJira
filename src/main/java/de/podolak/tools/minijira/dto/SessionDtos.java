package de.podolak.tools.minijira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SessionDtos {
    private SessionDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 80) String username,
            @Size(max = 120) String displayName,
            @Size(max = 120) String office,
            @NotBlank String theme
    ) {
    }

    public record UpdatePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(max = 255) String newPassword
    ) {
    }

    public record SessionDto(boolean loggedIn, Integer userId, String username, String displayName, String office, String theme) {
        public static SessionDto anonymous() {
            return new SessionDto(false, null, null, null, null, "light");
        }
    }
}
