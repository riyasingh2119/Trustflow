package com.trustflow.risk;

import com.trustflow.context.DeviceStatus;
import com.trustflow.resource.Sensitivity;

/**
 * Deterministic, rule-based and explainable risk engine.
 * score = device + resource + time + authentication, mapped to a decision by two thresholds.
 */
public class RiskEngine {

    private final RiskParams p;

    public RiskEngine(RiskParams params) {
        this.p = params;
    }

    public RiskResult evaluate(DeviceStatus device, Sensitivity sensitivity, int hour, int failedLogins) {
        int deviceRisk = switch (device) {
            case MANAGED_COMPLIANT -> 0;
            case UNKNOWN -> p.deviceUnknown();
            case NON_COMPLIANT -> p.deviceNonCompliant();
        };
        int resourceRisk = switch (sensitivity) {
            case LOW -> p.resourceLow();
            case MEDIUM -> p.resourceMedium();
            case HIGH -> p.resourceHigh();
        };
        int timeRisk = timeRisk(hour);
        int authRisk = Math.min(Math.max(failedLogins, 0) * p.authPerFailure(), p.authCap());

        FactorBreakdown factors = new FactorBreakdown(deviceRisk, resourceRisk, timeRisk, authRisk);
        int score = factors.total();
        Decision decision = decide(score);
        return new RiskResult(score, factors, decision,
                explain(device, sensitivity, factors, failedLogins, score, decision));
    }

    /** 09:00-18:00 = 0, 06:00-09:00 and 18:00-22:00 = shoulder, 22:00-06:00 = night. */
    int timeRisk(int hour) {
        if (hour >= 9 && hour < 18) return 0;
        if ((hour >= 6 && hour < 9) || (hour >= 18 && hour < 22)) return p.timeShoulder();
        return p.timeNight();
    }

    public Decision decide(int score) {
        if (score <= p.allowMax()) return Decision.ALLOW;
        if (score <= p.mfaMax()) return Decision.MFA_REQUIRED;
        return Decision.DENY;
    }

    private String explain(DeviceStatus device, Sensitivity s, FactorBreakdown f,
                           int failed, int score, Decision d) {
        StringBuilder sb = new StringBuilder();
        if (f.device() > 0) sb.append("Device ").append(device).append(" (+").append(f.device()).append("), ");
        sb.append(f.device() > 0 ? "resource " : "Resource ")
          .append(s).append(" (+").append(f.resource()).append("), ");
        if (f.time() > 0) sb.append("off-hours (+").append(f.time()).append("), ");
        if (f.auth() > 0) sb.append(failed).append(" recent failed login(s) (+").append(f.auth()).append("), ");
        String band = switch (d) {
            case ALLOW -> "ALLOW";
            case MFA_REQUIRED -> "MFA";
            case DENY -> "DENY";
        };
        sb.append("score ").append(score).append(" in ").append(band).append(" band");
        return sb.toString();
    }
}
