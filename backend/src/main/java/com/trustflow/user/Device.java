package com.trustflow.user;

import com.trustflow.context.DeviceStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices")
public class Device {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String fingerprint;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;
    @Column(name = "last_seen")
    private Instant lastSeen;

    protected Device() { }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getFingerprint() { return fingerprint; }
    public DeviceStatus getStatus() { return status; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
}
