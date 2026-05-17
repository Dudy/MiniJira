package de.podolak.tools.minijira.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AppUserTest {
    @Test
    void constructorStoresUsernameAndPassword() {
        AppUser user = new AppUser("alice", "secret");

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isEqualTo("secret");
    }

    @Test
    void settersUpdateMutableProfileFields() {
        AppUser user = new AppUser("alice", "secret");

        user.setUsername("bob");
        user.setPassword("new-secret");
        user.setDisplayName("Bob Example");
        user.setOffice("Berlin");
        user.setTheme(UserTheme.DARK);

        assertThat(user.getUsername()).isEqualTo("bob");
        assertThat(user.getPassword()).isEqualTo("new-secret");
        assertThat(user.getDisplayName()).isEqualTo("Bob Example");
        assertThat(user.getOffice()).isEqualTo("Berlin");
        assertThat(user.getTheme()).isEqualTo(UserTheme.DARK);
    }
}
