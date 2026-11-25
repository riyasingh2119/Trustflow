package com.trustflow.audit;

import com.trustflow.context.DeviceStatus;
import com.trustflow.policy.PolicyAction;
import com.trustflow.risk.Decision;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** Append-only audit record (Hibernate @Immutable, and no update or delete endpoints exist). */
@Entity
@Immutable
@Table(name = "audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp = Instant.now();
    @Column(nullable = false)
    private String username;
    @Column(name = "resource_id")
    private Long resourceId;
    @Column(name = "resource_name")
    private String resourceName;
    @Enumerated(EnumType.STRING)
    private PolicyAction action;
    @Column(name = "risk_score", nullable = false)
    private int riskScore;
    @Column(name = "device_risk", nullable = false)
    private int deviceRisk;
    @Column(name = "resource_risk", nullable = false)
    private int resourceRisk;
    @Column(name = "time_risk", nullable = false)
    private int timeRisk;
    @Column(name = "auth_risk", nullable = false)
    private int authRisk;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Decision decision;
    @Enumerated(EnumType.STRING) @Column(name = "device_status")
    private DeviceStatus deviceStatus;
    private String ip;
    private String reason;

    protected AuditEvent() { }

    public AuditEvent(String username, Long resourceId, String resourceName, PolicyAction action,
                      int riskScore, int deviceRisk, int resourceRisk, int timeRisk, int authRisk,
                      Decision decision, DeviceStatus deviceStatus, String ip, String reason) {
        this.username = username; this.resourceId = resourceId; this.resourceName = resourceName;
        this.action = action; this.riskScore = riskScore; this.deviceRisk = deviceRisk;
        this.resourceRisk = resourceRisk; this.timeRisk = timeRisk; this.authRisk = authRisk;
        this.decision = decision; this.deviceStatus = deviceStatus; this.ip = ip; this.reason = reason;
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getUsername() { return username; }
    public Long getResourceId() { return resourceId; }
    public String getResourceName() { return resourceName; }
    public PolicyAction getAction() { return action; }
    public int getRiskScore() { return riskScore; }
    public int getDeviceRisk() { return deviceRisk; }
    public int getResourceRisk() { return resourceRisk; }
    public int getTimeRisk() { return timeRisk; }
    public int getAuthRisk() { return authRisk; }
    public Decision getDecision() { return decision; }
    public DeviceStatus getDeviceStatus() { return deviceStatus; }
    public String getIp() { return ip; }
    public String getReason() { return reason; }
}
