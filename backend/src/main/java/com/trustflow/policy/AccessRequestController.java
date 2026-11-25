package com.trustflow.policy;

import com.trustflow.user.Role;
import com.trustflow.user.User;
import com.trustflow.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Temporary-access workflow: POST /api/requests, PUT /api/requests/{id}/approve (ADMIN). */
@RestController
@RequestMapping("/api/requests")
public class AccessRequestController {

    public record RequestView(Long id, Long userId, String username, Long resourceId, PolicyAction action,
                              String reason, AccessRequest.Status status, String approvedBy,
                              Instant expiresAt, Instant createdAt) { }

    public record CreateRequest(@NotNull Long resourceId, @NotNull PolicyAction action, String reason) { }

    public record Decision(boolean approve, @Min(1) @Max(168) Integer hours) { }

    private final AccessRequestRepository repo;
    private final PolicyRepository policies;
    private final UserRepository users;

    public AccessRequestController(AccessRequestRepository repo, PolicyRepository policies, UserRepository users) {
        this.repo = repo;
        this.policies = policies;
        this.users = users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView create(Authentication auth, @Valid @RequestBody CreateRequest q) {
        User u = users.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return view(repo.save(new AccessRequest(u.getId(), q.resourceId(), q.action(), q.reason())), u.getUsername());
    }

    /** Admins see everything, other users see their own requests. */
    @GetMapping
    public List<RequestView> list(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);
        User me = users.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        List<AccessRequest> rows = admin ? repo.findAllByOrderByCreatedAtDesc() : repo.findByUserIdOrderByCreatedAtDesc(me.getId());
        return rows.stream().map(r -> view(r, users.findById(r.getUserId()).map(User::getUsername).orElse("?"))).toList();
    }

    @PutMapping("/{id}/approve")
    @Transactional
    public RequestView decide(@PathVariable Long id, Authentication auth, @Valid @RequestBody Decision d) {
        AccessRequest r = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (r.getStatus() != AccessRequest.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already " + r.getStatus());
        }
        User requester = users.findById(r.getUserId()).orElseThrow();
        r.setApprovedBy(auth.getName());
        if (d.approve()) {
            Instant now = Instant.now();
            Instant until = now.plus(Duration.ofHours(d.hours() == null ? 8 : d.hours()));
            Role anyRole = requester.getRoles().stream().findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requester has no role"));
            // Time-bound, single-user policy.
            policies.save(Policy.of(r.getResourceId(), anyRole.getId(), requester.getId(), r.getAction(),
                    PolicyStatus.ACTIVE, now, until, "Temporary access request #" + r.getId()));
            r.setStatus(AccessRequest.Status.APPROVED);
            r.setExpiresAt(until);
        } else {
            r.setStatus(AccessRequest.Status.REJECTED);
        }
        return view(repo.save(r), requester.getUsername());
    }

    private RequestView view(AccessRequest r, String username) {
        AccessRequest.Status s = r.getStatus();
        if (s == AccessRequest.Status.APPROVED && r.getExpiresAt() != null && Instant.now().isAfter(r.getExpiresAt())) {
            s = AccessRequest.Status.EXPIRED;
        }
        return new RequestView(r.getId(), r.getUserId(), username, r.getResourceId(), r.getAction(), r.getReason(),
                s, r.getApprovedBy(), r.getExpiresAt(), r.getCreatedAt());
    }
}
