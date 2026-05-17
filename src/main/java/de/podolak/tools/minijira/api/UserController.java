package de.podolak.tools.minijira.api;

import de.podolak.tools.minijira.domain.AppUser;
import de.podolak.tools.minijira.dto.UserDtos.CreateUserRequest;
import de.podolak.tools.minijira.dto.UserDtos.UserDto;
import de.podolak.tools.minijira.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        AppUser user = userService.createUser(request.username().trim(), request.password());
        return ResponseEntity.created(URI.create("/api/users/" + user.getId()))
                .body(DtoMapper.toUserDto(user));
    }

    @GetMapping
    public List<UserDto> listUsers() {
        return userService.listUsers().stream().map(DtoMapper::toUserDto).toList();
    }
}
