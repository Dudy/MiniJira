package de.podolak.tools.minijira.api;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.UserTheme;
import de.podolak.tools.minijira.dto.SessionDtos.LoginRequest;
import de.podolak.tools.minijira.dto.SessionDtos.SessionDto;
import de.podolak.tools.minijira.dto.SessionDtos.UpdatePasswordRequest;
import de.podolak.tools.minijira.dto.SessionDtos.UpdateProfileRequest;
import de.podolak.tools.minijira.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {
    public static final String SESSION_USER_ID = "userId";
    public static final String SESSION_USERNAME = "username";

    private final UserService userService;

    public SessionController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public SessionDto login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        AppUser user = userService.authenticate(request.username().trim(), request.password());
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_USERNAME, user.getUsername());
        return toSessionDto(user);
    }

    @GetMapping
    public SessionDto currentSession(HttpSession session) {
        Object userId = session.getAttribute(SESSION_USER_ID);
        if (userId instanceof Integer id) {
            return toSessionDto(userService.getById(id));
        }
        return SessionDto.anonymous();
    }

    @DeleteMapping
    public SessionDto logout(HttpSession session) {
        session.invalidate();
        return SessionDto.anonymous();
    }

    @PutMapping("/profile")
    public SessionDto updateProfile(@Valid @RequestBody UpdateProfileRequest request, HttpSession session) {
        Integer userId = currentUserId(session);
        AppUser user = userService.updateProfile(
                userId,
                request.username().trim(),
                request.displayName(),
                request.office(),
                request.theme()
        );
        session.setAttribute(SESSION_USERNAME, user.getUsername());
        return toSessionDto(user);
    }

    @PutMapping("/password")
    public SessionDto updatePassword(@Valid @RequestBody UpdatePasswordRequest request, HttpSession session) {
        Integer userId = currentUserId(session);
        userService.updatePassword(userId, request.currentPassword(), request.newPassword());
        return toSessionDto(userService.getById(userId));
    }

    private Integer currentUserId(HttpSession session) {
        Object userId = session.getAttribute(SESSION_USER_ID);
        if (userId instanceof Integer id) {
            return id;
        }
        throw new de.podolak.tools.minijira.service.AuthenticationException("Not logged in");
    }

    private SessionDto toSessionDto(AppUser user) {
        return new SessionDto(
                true,
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getOffice(),
                toThemeValue(user.getTheme())
        );
    }

    private String toThemeValue(UserTheme theme) {
        return theme == null ? UserTheme.LIGHT.name().toLowerCase() : theme.name().toLowerCase();
    }
}
