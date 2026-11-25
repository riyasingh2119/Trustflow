package com.trustflow.policy;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PolicyEvaluatorTest {

    private final Instant now = Instant.parse("2026-03-10T10:00:00Z");

    private Policy policy(Long roleId, Long userId, PolicyStatus status, Instant from, Instant until) {
        return Policy.of(1L, roleId, userId, PolicyAction.READ, status, from, until, null);
    }

    @Test
    void activePolicyWithoutWindowMatchesRole() {
        assertTrue(PolicyEvaluator.matches(policy(2L, null, PolicyStatus.ACTIVE, null, null), 7L, Set.of(2L), now));
    }

    @Test
    void differentRoleDoesNotMatch() {
        assertFalse(PolicyEvaluator.matches(policy(3L, null, PolicyStatus.ACTIVE, null, null), 7L, Set.of(2L), now));
    }

    @Test
    void disabledPolicyNeverMatches() {
        assertFalse(PolicyEvaluator.matches(policy(2L, null, PolicyStatus.DISABLED, null, null), 7L, Set.of(2L), now));
    }

    @Test
    void policyNotYetValidDoesNotMatch() {
        assertFalse(PolicyEvaluator.matches(policy(2L, null, PolicyStatus.ACTIVE, now.plus(Duration.ofHours(1)), null), 7L, Set.of(2L), now));
    }

    @Test
    void expiredPolicyDoesNotMatch() {
        assertFalse(PolicyEvaluator.matches(policy(2L, null, PolicyStatus.ACTIVE, null, now.minusSeconds(1)), 7L, Set.of(2L), now));
    }

    @Test
    void policyInsideItsWindowMatches() {
        assertTrue(PolicyEvaluator.matches(policy(2L, null, PolicyStatus.ACTIVE, now.minusSeconds(60), now.plusSeconds(60)), 7L, Set.of(2L), now));
    }

    @Test
    void temporaryUserPolicyOnlyMatchesThatUser() {
        Policy p = policy(2L, 7L, PolicyStatus.ACTIVE, now.minusSeconds(60), now.plusSeconds(60));
        assertTrue(PolicyEvaluator.matches(p, 7L, Set.of(2L), now));
        assertFalse(PolicyEvaluator.matches(p, 8L, Set.of(2L), now));
    }
}
