package de.podolak.tools.minijira.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.dto.UserDtos.CreateUserRequest;
import de.podolak.tools.minijira.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock
    private UserService userService;

    @Test
    void createUserTrimsUsernameAndReturnsCreatedResponse() {
        UserController controller = new UserController(userService);
        AppUser user = userWithId(4, "alice");
        when(userService.createUser("alice", "secret")).thenReturn(user);

        var response = controller.createUser(new CreateUserRequest(" alice ", "secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation().toString()).isEqualTo("/api/users/4");
        assertThat(response.getBody().id()).isEqualTo(4);
        assertThat(response.getBody().username()).isEqualTo("alice");
        verify(userService).createUser("alice", "secret");
    }

    @Test
    void listUsersMapsUsersToDtos() {
        UserController controller = new UserController(userService);
        when(userService.listUsers()).thenReturn(List.of(userWithId(1, "alice"), userWithId(2, "bob")));

        var users = controller.listUsers();

        assertThat(users).extracting("username").containsExactly("alice", "bob");
        assertThat(users).extracting("id").containsExactly(1, 2);
    }

    private static AppUser userWithId(Integer id, String username) {
        AppUser user = new AppUser(username, "secret");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
