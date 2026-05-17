package de.podolak.tools.minijira.dto;

import jakarta.validation.constraints.NotBlank;

public final class SessionDtos {
    private SessionDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record SessionDto(boolean loggedIn, Integer userId, String username) {
        public static SessionDto anonymous() {
            return new SessionDto(false, null, null);
        }
    }
}
