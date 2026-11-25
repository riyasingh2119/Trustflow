package com.trustflow.risk;

import java.util.Map;

/** Weights and thresholds. Stored in the risk_config table so an admin can tune them. */
public record RiskParams(
        int deviceUnknown, int deviceNonCompliant,
        int resourceLow, int resourceMedium, int resourceHigh,
        int timeShoulder, int timeNight,
        int authPerFailure, int authCap, int authWindowMinutes,
        int allowMax, int mfaMax) {

    public static RiskParams defaults() {
        return new RiskParams(15, 30, 5, 15, 30, 7, 15, 5, 25, 15, 30, 60);
    }

    public static RiskParams fromMap(Map<String, Integer> m) {
        RiskParams d = defaults();
        return new RiskParams(
                m.getOrDefault("device.unknown", d.deviceUnknown),
                m.getOrDefault("device.noncompliant", d.deviceNonCompliant),
                m.getOrDefault("resource.low", d.resourceLow),
                m.getOrDefault("resource.medium", d.resourceMedium),
                m.getOrDefault("resource.high", d.resourceHigh),
                m.getOrDefault("time.shoulder", d.timeShoulder),
                m.getOrDefault("time.night", d.timeNight),
                m.getOrDefault("auth.per_failure", d.authPerFailure),
                m.getOrDefault("auth.cap", d.authCap),
                m.getOrDefault("auth.window_minutes", d.authWindowMinutes),
                m.getOrDefault("threshold.allow_max", d.allowMax),
                m.getOrDefault("threshold.mfa_max", d.mfaMax));
    }
}
