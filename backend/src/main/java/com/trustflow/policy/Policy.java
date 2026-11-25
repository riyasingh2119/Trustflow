package com.trustflow.policy;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "policies")
public class Policy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    /** Optional: when set, the policy applies to this single user (temporary access grants). */
    @Column(name = "user_id")
    private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PolicyAction action;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PolicyStatus status = PolicyStatus.ACTIVE;
    @Column(name = "valid_from")
    private Instant validFrom;
    @Column(name = "valid_until")
    private Instant validUntil;
    private String conditions;

    protected Policy() { }

    public static Policy of(Long resourceId, Long roleId, Long userId, PolicyAction action,
                            PolicyStatus status, Instant from, Instant until, String conditions) {
        Policy p = new Policy();
        p.resourceId = resourceId; p.roleId = roleId; p.userId = userId; p.action = action;
        p.status = status; p.validFrom = from; p.validUntil = until; p.conditions = conditions;
        return p;
    }

    public Long getId() { return id; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long v) { this.resourceId = v; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long v) { this.roleId = v; }
    public Long getUserId() { return userId; }
    public PolicyAction getAction() { return action; }
    public void setAction(PolicyAction v) { this.action = v; }
    public PolicyStatus getStatus() { return status; }
    public void setStatus(PolicyStatus v) { this.status = v; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant v) { this.validFrom = v; }
    public Instant getValidUntil() { return validUntil; }
    public void setValidUntil(Instant v) { this.validUntil = v; }
    public String getConditions() { return conditions; }
    public void setConditions(String v) { this.conditions = v; }
}
