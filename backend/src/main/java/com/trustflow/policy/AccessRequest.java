package com.trustflow.policy;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "access_requests")
public class AccessRequest {
    public enum Status { PENDING, APPROVED, REJECTED, EXPIRED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PolicyAction action;
    private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.PENDING;
    @Column(name = "approved_by")
    private String approvedBy;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AccessRequest() { }
    public AccessRequest(Long userId, Long resourceId, PolicyAction action, String reason) {
        this.userId = userId; this.resourceId = resourceId; this.action = action; this.reason = reason;
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getResourceId() { return resourceId; }
    public PolicyAction getAction() { return action; }
    public String getReason() { return reason; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String a) { this.approvedBy = a; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant e) { this.expiresAt = e; }
    public Instant getCreatedAt() { return createdAt; }
}
