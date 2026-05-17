package de.podolak.tools.minijira.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(name = "uk_app_user_username", columnNames = "username"))
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 80)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 120)
    private String displayName;

    @Column(length = 120)
    private String office;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private UserTheme theme;

    protected AppUser() {
    }

    public AppUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public UserTheme getTheme() {
        return theme;
    }

    public void setTheme(UserTheme theme) {
        this.theme = theme;
    }
}
