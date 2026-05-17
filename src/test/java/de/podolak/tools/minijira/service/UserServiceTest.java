package de.podolak.tools.minijira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.UserTheme;
import de.podolak.tools.minijira.repo.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Test
    void createUserSavesNewUserWithLightTheme() {
        UserService service = new UserService(userRepository);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser created = service.createUser("alice", "secret");

        assertThat(created.getUsername()).isEqualTo("alice");
        assertThat(created.getPassword()).isEqualTo("secret");
        assertThat(created.getTheme()).isEqualTo(UserTheme.LIGHT);
        verify(userRepository).save(created);
    }

    @Test
    void createUserRejectsDuplicateUsername() {
        UserService service = new UserService(userRepository);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser("alice", "secret"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already exists: alice");
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateReturnsUserWhenPasswordMatches() {
        UserService service = new UserService(userRepository);
        AppUser user = new AppUser("alice", "secret");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThat(service.authenticate("alice", "secret")).isSameAs(user);
    }

    @Test
    void authenticateRejectsUnknownUserAndWrongPassword() {
        UserService service = new UserService(userRepository);
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(new AppUser("alice", "secret")));

        assertThatThrownBy(() -> service.authenticate("missing", "secret"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid username or password");
        assertThatThrownBy(() -> service.authenticate("alice", "wrong"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void getByIdReturnsUserOrThrowsNotFound() {
        UserService service = new UserService(userRepository);
        AppUser user = userWithId(7, "alice");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(userRepository.findById(8)).thenReturn(Optional.empty());

        assertThat(service.getById(7)).isSameAs(user);
        assertThatThrownBy(() -> service.getById(8))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found: 8");
    }

    @Test
    void updateProfileChangesUsernameProfileFieldsAndTheme() {
        UserService service = new UserService(userRepository);
        AppUser user = userWithId(7, "alice");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());

        AppUser updated = service.updateProfile(7, "bob", " Bob Example ", " Berlin ", "dark");

        assertThat(updated).isSameAs(user);
        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getDisplayName()).isEqualTo("Bob Example");
        assertThat(user.getOffice()).isEqualTo("Berlin");
        assertThat(user.getTheme()).isEqualTo(UserTheme.DARK);
    }

    @Test
    void updateProfileKeepsUsernameWhenUnchangedAndAllowsNullProfileFields() {
        UserService service = new UserService(userRepository);
        AppUser user = userWithId(7, "alice");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        service.updateProfile(7, "alice", null, null, "RETRO");

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getDisplayName()).isNull();
        assertThat(user.getOffice()).isNull();
        assertThat(user.getTheme()).isEqualTo(UserTheme.RETRO);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void updateProfileRejectsExistingUsernameOwnedByAnotherUser() {
        UserService service = new UserService(userRepository);
        AppUser user = userWithId(7, "alice");
        AppUser existing = userWithId(8, "bob");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateProfile(7, "bob", null, null, "light"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already exists: bob");
    }

    @Test
    void updateProfileAllowsExistingUsernameWhenItIsSameUser() {
        UserService service = new UserService(userRepository);
        AppUser user = userWithId(7, "alice");
        AppUser sameUser = userWithId(7, "bob");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(sameUser));

        service.updateProfile(7, "bob", null, null, "light");

        assertThat(user.getUsername()).isEqualTo("bob");
    }

    @Test
    void updateProfileRejectsMissingUserAndInvalidTheme() {
        UserService service = new UserService(userRepository);
        AppUser user = userWithId(7, "alice");
        when(userRepository.findById(404)).thenReturn(Optional.empty());
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateProfile(404, "alice", null, null, "light"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found: 404");
        assertThatThrownBy(() -> service.updateProfile(7, "alice", null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Theme is required");
        assertThatThrownBy(() -> service.updateProfile(7, "alice", null, null, "blue"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported theme: blue");
    }

    @Test
    void updatePasswordChangesPasswordWhenCurrentPasswordMatches() {
        UserService service = new UserService(userRepository);
        AppUser user = userWithId(7, "alice");
        user.setPassword("old");
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        AppUser updated = service.updatePassword(7, "old", "new");

        assertThat(updated).isSameAs(user);
        assertThat(user.getPassword()).isEqualTo("new");
    }

    @Test
    void updatePasswordRejectsMissingUserAndWrongCurrentPassword() {
        UserService service = new UserService(userRepository);
        AppUser user = userWithId(7, "alice");
        user.setPassword("old");
        when(userRepository.findById(404)).thenReturn(Optional.empty());
        when(userRepository.findById(7)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updatePassword(404, "old", "new"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found: 404");
        assertThatThrownBy(() -> service.updatePassword(7, "wrong", "new"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid current password");
        assertThat(user.getPassword()).isEqualTo("old");
    }

    @Test
    void listUsersDelegatesWithUsernameAscendingSort() {
        UserService service = new UserService(userRepository);
        AppUser alice = new AppUser("alice", "secret");
        when(userRepository.findAll(any(Sort.class))).thenReturn(List.of(alice));

        assertThat(service.listUsers()).containsExactly(alice);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(userRepository).findAll(sortCaptor.capture());
        assertThat(sortCaptor.getValue()).isEqualTo(Sort.by(Sort.Direction.ASC, "username"));
    }

    private static AppUser userWithId(Integer id, String username) {
        AppUser user = new AppUser(username, "secret");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
