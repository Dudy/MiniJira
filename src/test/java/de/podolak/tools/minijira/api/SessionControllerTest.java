package de.podolak.tools.minijira.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.UserTheme;
import de.podolak.tools.minijira.dto.SessionDtos.LoginRequest;
import de.podolak.tools.minijira.dto.SessionDtos.UpdatePasswordRequest;
import de.podolak.tools.minijira.dto.SessionDtos.UpdateProfileRequest;
import de.podolak.tools.minijira.service.AuthenticationException;
import de.podolak.tools.minijira.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {
    @Mock
    private UserService userService;

    @Mock
    private HttpSession session;

    @Test
    void loginAuthenticatesTrimmedUsernameAndStoresSessionAttributes() {
        SessionController controller = new SessionController(userService);
        AppUser user = userWithId(7, "alice");
        user.setDisplayName("Alice");
        user.setOffice("Berlin");
        user.setTheme(UserTheme.DARK);
        when(userService.authenticate("alice", "secret")).thenReturn(user);

        var dto = controller.login(new LoginRequest(" alice ", "secret"), session);

        assertThat(dto.loggedIn()).isTrue();
        assertThat(dto.userId()).isEqualTo(7);
        assertThat(dto.username()).isEqualTo("alice");
        assertThat(dto.displayName()).isEqualTo("Alice");
        assertThat(dto.office()).isEqualTo("Berlin");
        assertThat(dto.theme()).isEqualTo("dark");
        verify(session).setAttribute(SessionController.SESSION_USER_ID, 7);
        verify(session).setAttribute(SessionController.SESSION_USERNAME, "alice");
    }

    @Test
    void currentSessionReturnsUserWhenSessionContainsIntegerUserId() {
        SessionController controller = new SessionController(userService);
        AppUser user = userWithId(7, "alice");
        when(session.getAttribute(SessionController.SESSION_USER_ID)).thenReturn(7);
        when(userService.getById(7)).thenReturn(user);

        var dto = controller.currentSession(session);

        assertThat(dto.loggedIn()).isTrue();
        assertThat(dto.userId()).isEqualTo(7);
        assertThat(dto.theme()).isEqualTo("light");
    }

    @Test
    void currentSessionReturnsAnonymousForMissingOrNonIntegerUserId() {
        SessionController controller = new SessionController(userService);
        when(session.getAttribute(SessionController.SESSION_USER_ID)).thenReturn(null, "7");

        assertThat(controller.currentSession(session).loggedIn()).isFalse();
        assertThat(controller.currentSession(session).loggedIn()).isFalse();
    }

    @Test
    void logoutInvalidatesSessionAndReturnsAnonymous() {
        SessionController controller = new SessionController(userService);

        var dto = controller.logout(session);

        assertThat(dto.loggedIn()).isFalse();
        assertThat(dto.theme()).isEqualTo("light");
        verify(session).invalidate();
    }

    @Test
    void updateProfileRequiresLoggedInUserAndUpdatesSessionUsername() {
        SessionController controller = new SessionController(userService);
        AppUser updated = userWithId(7, "bob");
        updated.setDisplayName("Bob");
        updated.setOffice("Munich");
        updated.setTheme(UserTheme.RETRO);
        when(session.getAttribute(SessionController.SESSION_USER_ID)).thenReturn(7);
        when(userService.updateProfile(7, "bob", "Bob", "Munich", "retro")).thenReturn(updated);

        var dto = controller.updateProfile(new UpdateProfileRequest(" bob ", "Bob", "Munich", "retro"), session);

        assertThat(dto.username()).isEqualTo("bob");
        assertThat(dto.theme()).isEqualTo("retro");
        verify(session).setAttribute(SessionController.SESSION_USERNAME, "bob");
    }

    @Test
    void updateProfileRejectsAnonymousSession() {
        SessionController controller = new SessionController(userService);
        when(session.getAttribute(SessionController.SESSION_USER_ID)).thenReturn(null);

        assertThatThrownBy(() -> controller.updateProfile(new UpdateProfileRequest("bob", null, null, "light"),
                session))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Not logged in");
    }

    @Test
    void updatePasswordRequiresLoggedInUserAndReturnsFreshSessionUser() {
        SessionController controller = new SessionController(userService);
        AppUser user = userWithId(7, "alice");
        when(session.getAttribute(SessionController.SESSION_USER_ID)).thenReturn(7);
        when(userService.getById(7)).thenReturn(user);

        var dto = controller.updatePassword(new UpdatePasswordRequest("old", "new"), session);

        verify(userService).updatePassword(7, "old", "new");
        assertThat(dto.loggedIn()).isTrue();
        assertThat(dto.userId()).isEqualTo(7);
    }

    @Test
    void updatePasswordRejectsAnonymousSession() {
        SessionController controller = new SessionController(userService);
        when(session.getAttribute(SessionController.SESSION_USER_ID)).thenReturn("not-an-integer");

        assertThatThrownBy(() -> controller.updatePassword(new UpdatePasswordRequest("old", "new"), session))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Not logged in");
    }

    private static AppUser userWithId(Integer id, String username) {
        AppUser user = new AppUser(username, "secret");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
