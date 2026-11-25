package com.trustflow.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String username;
    private String ip;
    @Column(nullable = false)
    private boolean success;
    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt = Instant.now();

    protected LoginAttempt() { }
    public LoginAttempt(String username, String ip, boolean success) {
        this.username = username;
        this.ip = ip;
        this.success = success;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public boolean isSuccess() { return success; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
