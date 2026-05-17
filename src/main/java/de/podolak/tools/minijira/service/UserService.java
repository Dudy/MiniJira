package de.podolak.tools.minijira.service;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.domain.UserTheme;
import de.podolak.tools.minijira.repo.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AppUser createUser(String username, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already exists: " + username);
        }
        AppUser user = new AppUser(username, password);
        user.setTheme(UserTheme.LIGHT);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AppUser authenticate(String username, String password) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));
        if (!user.getPassword().equals(password)) {
            throw new AuthenticationException("Invalid username or password");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public AppUser getById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional
    public AppUser updateProfile(Integer userId, String username, String displayName, String office, String theme) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (!user.getUsername().equals(username)) {
            userRepository.findByUsername(username)
                    .filter(existing -> !existing.getId().equals(userId))
                    .ifPresent(existing -> {
                        throw new BadRequestException("Username already exists: " + username);
                    });
            user.setUsername(username);
        }
        user.setDisplayName(normalize(displayName));
        user.setOffice(normalize(office));
        user.setTheme(parseTheme(theme));
        return user;
    }

    @Transactional
    public AppUser updatePassword(Integer userId, String currentPassword, String newPassword) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        if (!user.getPassword().equals(currentPassword)) {
            throw new AuthenticationException("Invalid current password");
        }
        user.setPassword(newPassword);
        return user;
    }

    @Transactional(readOnly = true)
    public List<AppUser> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "username"));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private UserTheme parseTheme(String value) {
        if (value == null) {
            throw new BadRequestException("Theme is required");
        }
        try {
            return UserTheme.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported theme: " + value);
        }
    }
}
