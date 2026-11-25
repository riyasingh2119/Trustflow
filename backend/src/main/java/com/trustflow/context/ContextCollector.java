package com.trustflow.context;

import com.trustflow.risk.RiskConfigService;
import com.trustflow.user.DeviceRepository;
import com.trustflow.user.LoginAttemptRepository;
import com.trustflow.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Gathers device status, time of access and recent failed logins for one request. */
@Component
public class ContextCollector {

    private final DeviceRepository devices;
    private final LoginAttemptRepository attempts;
    private final RiskConfigService config;
    private final ZoneId zone;
    private final boolean demoMode;

    public ContextCollector(DeviceRepository devices, LoginAttemptRepository attempts, RiskConfigService config,
                            @Value("${trustflow.timezone}") String tz,
                            @Value("${trustflow.demo-mode:true}") boolean demoMode) {
        this.devices = devices;
        this.attempts = attempts;
        this.config = config;
        this.zone = ZoneId.of(tz);
        this.demoMode = demoMode;
    }

    public AccessContext collect(User user, String deviceId, Integer simulatedHour, String ip) {
        return new AccessContext(deviceStatus(user, deviceId), hour(simulatedHour), failedLogins(user), ip);
    }

    private DeviceStatus deviceStatus(User user, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) return DeviceStatus.UNKNOWN;
        return devices.findByUserIdAndFingerprint(user.getId(), deviceId.trim())
                .map(d -> {
                    d.setLastSeen(Instant.now());
                    devices.save(d);
                    return d.getStatus();
                })
                .orElse(DeviceStatus.UNKNOWN);
    }

    /** The demo-mode override lets the viva demo show evening and night scenarios at any time of day. */
    private int hour(Integer simulatedHour) {
        if (demoMode && simulatedHour != null && simulatedHour >= 0 && simulatedHour <= 23) return simulatedHour;
        return ZonedDateTime.now(zone).getHour();
    }

    private int failedLogins(User user) {
        Instant since = Instant.now().minus(Duration.ofMinutes(config.params().authWindowMinutes()));
        return (int) attempts.countByUsernameAndSuccessFalseAndAttemptedAtAfter(user.getUsername(), since);
    }
}
