package de.podolak.tools.minijira.api;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.dto.SessionDtos.LoginRequest;
import de.podolak.tools.minijira.dto.SessionDtos.SessionDto;
import de.podolak.tools.minijira.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
        return new SessionDto(true, user.getId(), user.getUsername());
    }

    @GetMapping
    public SessionDto currentSession(HttpSession session) {
        Object userId = session.getAttribute(SESSION_USER_ID);
        Object username = session.getAttribute(SESSION_USERNAME);
        if (userId instanceof Integer id && username instanceof String name) {
            return new SessionDto(true, id, name);
        }
        return SessionDto.anonymous();
    }

    @DeleteMapping
    public SessionDto logout(HttpSession session) {
        session.invalidate();
        return SessionDto.anonymous();
    }
}
