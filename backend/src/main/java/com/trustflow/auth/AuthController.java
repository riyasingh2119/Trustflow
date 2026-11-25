package com.trustflow.auth;

import com.trustflow.common.ClientIp;
import com.trustflow.common.TrustFlowMetrics;
import com.trustflow.policy.PolicyAction;
import com.trustflow.user.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record RegisterRequest(@NotBlank @Size(min = 3, max = 64) String username,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) { }

    public record LoginRequest(@NotBlank @Size(max = 64) String username, @NotBlank @Size(max = 72) String password) { }

    public record LoginResponse(String token, String username, List<String> roles, long expiresInSeconds) { }

    public record MfaVerifyRequest(@NotBlank String challengeId, @NotBlank String code) { }

    private final UserRepository users;
    private final LoginAttemptRepository attempts;
    private final DeviceRepository devices;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final MfaService mfa;
    private final TrustFlowMetrics metrics;
    private final boolean demoMode;
    private final String dummyHash;

    public AuthController(UserRepository users, LoginAttemptRepository attempts, DeviceRepository devices,
                          PasswordEncoder encoder, JwtService jwt, MfaService mfa, TrustFlowMetrics metrics,
                          @Value("${trustflow.demo-mode:true}") boolean demoMode) {
        this.users = users;
        this.attempts = attempts;
        this.devices = devices;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mfa = mfa;
        this.metrics = metrics;
        this.demoMode = demoMode;
        this.dummyHash = encoder.encode("not-a-real-password"); // equalises timing for unknown usernames
    }

    /** Self-registration creates a user with no roles; an admin assigns roles afterwards (least privilege). */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest q) {
        if (users.existsByUsername(q.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        try {
            User u = users.save(new User(q.username(), q.email(), encoder.encode(q.password()), AdminUserController.newSecret()));
            return Map.of("id", u.getId(), "username", u.getUsername(), "roles", List.of());
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest q, HttpServletRequest req) {
        var found = users.findByUsername(q.username());
        boolean passwordOk = encoder.matches(q.password(), found.map(User::getPasswordHash).orElse(dummyHash));
        boolean ok = found.isPresent() && found.get().isEnabled() && passwordOk;
        attempts.save(new LoginAttempt(q.username(), ClientIp.of(req), ok));
        if (!ok) {
            metrics.failedLogin();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        User u = found.get();
        List<String> roles = u.getRoles().stream().map(Role::getName).sorted().toList();
        return new LoginResponse(jwt.accessToken(u.getUsername(), roles), u.getUsername(), roles, jwt.accessTtlSeconds());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        User u = currentUser(auth);
        return Map.of(
                "username", u.getUsername(),
                "email", u.getEmail(),
                "roles", u.getRoles().stream().map(Role::getName).sorted().toList(),
                "devices", devices.findByUserId(u.getId()).stream()
                        .map(d -> Map.of("fingerprint", d.getFingerprint(), "status", d.getStatus().name())).toList());
    }

    /** Submit the one-time code for a challenge. Success returns a single-use token for the retry. */
    @PostMapping("/mfa/verify")
    public Map<String, Object> verify(Authentication auth, @Valid @RequestBody MfaVerifyRequest q) {
        User u = currentUser(auth);
        Claims c;
        try {
            c = jwt.parse(q.challengeId());
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge is invalid or expired; request access again");
        }
        if (!JwtService.CHALLENGE.equals(c.get("tt", String.class)) || !u.getUsername().equals(c.getSubject())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge does not belong to this user");
        }
        if (!mfa.verify(u.getMfaSecret(), q.code())) {
            metrics.mfa("failed");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect or expired one-time code");
        }
        metrics.mfa("passed");
        Long rid = JwtService.longClaim(c, "rid");
        PolicyAction act = PolicyAction.valueOf(c.get("act", String.class));
        return Map.of("mfaToken", jwt.mfaToken(u.getUsername(), rid, act), "validForSeconds", 120);
    }

    /**
     * Demo only: stands in for the user's authenticator app so the flow can be shown without a phone.
     * Disabled when trustflow.demo-mode=false.
     */
    @GetMapping("/mfa/demo-code")
    public Map<String, Object> demoCode(Authentication auth) {
        if (!demoMode) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        User u = currentUser(auth);
        return Map.of("code", mfa.currentCode(u.getMfaSecret()), "secondsRemaining", mfa.secondsRemaining());
    }

    private User currentUser(Authentication auth) {
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }
}
