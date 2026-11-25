package com.trustflow.policy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/** CRUD /api/admin/policies, including time-limited policies (ADMIN only). */
@RestController
@RequestMapping("/api/admin/policies")
public class AdminPolicyController {

    public record PolicyView(Long id, Long resourceId, Long roleId, Long userId, PolicyAction action,
                             PolicyStatus status, Instant validFrom, Instant validUntil, String conditions) {
        static PolicyView of(Policy p) {
            return new PolicyView(p.getId(), p.getResourceId(), p.getRoleId(), p.getUserId(), p.getAction(),
                    p.getStatus(), p.getValidFrom(), p.getValidUntil(), p.getConditions());
        }
    }

    public record PolicyRequest(@NotNull Long resourceId, @NotNull Long roleId, @NotNull PolicyAction action,
                                PolicyStatus status, Instant validFrom, Instant validUntil, String conditions) { }

    private final PolicyRepository repo;

    public AdminPolicyController(PolicyRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<PolicyView> list() {
        return repo.findAll().stream().map(PolicyView::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyView create(@Valid @RequestBody PolicyRequest q) {
        validate(q);
        return PolicyView.of(repo.save(Policy.of(q.resourceId(), q.roleId(), null, q.action(),
                q.status() == null ? PolicyStatus.ACTIVE : q.status(), q.validFrom(), q.validUntil(), q.conditions())));
    }

    @PutMapping("/{id}")
    public PolicyView update(@PathVariable Long id, @Valid @RequestBody PolicyRequest q) {
        validate(q);
        Policy p = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));
        p.setResourceId(q.resourceId()); p.setRoleId(q.roleId()); p.setAction(q.action());
        if (q.status() != null) p.setStatus(q.status());
        p.setValidFrom(q.validFrom()); p.setValidUntil(q.validUntil()); p.setConditions(q.conditions());
        return PolicyView.of(repo.save(p));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
        repo.deleteById(id);
    }

    private void validate(PolicyRequest q) {
        if (q.validFrom() != null && q.validUntil() != null && !q.validUntil().isAfter(q.validFrom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validUntil must be after validFrom");
        }
    }
}
