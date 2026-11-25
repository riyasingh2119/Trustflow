package com.trustflow.policy;

import com.trustflow.user.Role;
import com.trustflow.user.User;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/** RBAC + time-bound ABAC gate. A request needs an ACTIVE, in-validity policy, otherwise reason NO_POLICY. */
@Component
public class PolicyEvaluator {

    private final PolicyRepository policies;

    public PolicyEvaluator(PolicyRepository policies) {
        this.policies = policies;
    }

    public boolean isGranted(User user, Long resourceId, PolicyAction action, Instant now) {
        Set<Long> roleIds = user.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        return policies.findByResourceIdAndAction(resourceId, action).stream()
                .anyMatch(p -> matches(p, user.getId(), roleIds, now));
    }

    /** Pure, unit-testable matching rule. */
    public static boolean matches(Policy p, Long userId, Set<Long> roleIds, Instant now) {
        if (p.getStatus() != PolicyStatus.ACTIVE) return false;
        if (!inValidityWindow(p, now)) return false;
        if (p.getUserId() != null) return p.getUserId().equals(userId);
        return roleIds.contains(p.getRoleId());
    }

    public static boolean inValidityWindow(Policy p, Instant now) {
        if (p.getValidFrom() != null && now.isBefore(p.getValidFrom())) return false;
        return p.getValidUntil() == null || now.isBefore(p.getValidUntil());
    }
}
