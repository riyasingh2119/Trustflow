package com.trustflow.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** CRUD /api/admin/users (ADMIN only, enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    public record UserView(Long id, String username, String email, boolean enabled, List<String> roles) {
        static UserView of(User u) {
            return new UserView(u.getId(), u.getUsername(), u.getEmail(), u.isEnabled(),
                    u.getRoles().stream().map(Role::getName).sorted().toList());
        }
    }

    public record CreateUser(@NotBlank @Size(min = 3, max = 64) String username,
                             @NotBlank @Email String email,
                             @NotBlank @Size(min = 8, max = 72) String password,
                             List<String> roles) { }

    public record UpdateUser(String email, Boolean enabled, List<String> roles,
                             @Size(min = 8, max = 72) String password) { }

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public AdminUserController(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<UserView> list() {
        return users.findAll().stream().map(UserView::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UserView create(@Valid @RequestBody CreateUser req) {
        if (users.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        User u = new User(req.username(), req.email(), encoder.encode(req.password()), newSecret());
        u.setRoles(resolveRoles(req.roles()));
        return UserView.of(users.save(u));
    }

    @PutMapping("/{id}")
    @Transactional
    public UserView update(@PathVariable Long id, @Valid @RequestBody UpdateUser req) {
        User u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (req.email() != null) u.setEmail(req.email());
        if (req.enabled() != null) u.setEnabled(req.enabled());
        if (req.roles() != null) u.setRoles(resolveRoles(req.roles()));
        if (req.password() != null) u.setPasswordHash(encoder.encode(req.password()));
        return UserView.of(users.save(u));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!users.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        users.deleteById(id);
    }

    private Set<Role> resolveRoles(List<String> names) {
        Set<Role> out = new HashSet<>();
        if (names == null) return out;
        for (String n : names) {
            out.add(roles.findByName(n.toUpperCase())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role: " + n)));
        }
        return out;
    }

    public static String newSecret() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        return sb.toString();
    }
}
