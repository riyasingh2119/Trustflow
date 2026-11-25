package com.trustflow.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Custom metrics exposed to Prometheus:
 * trustflow_decisions_total{decision}, trustflow_risk_score, trustflow_mfa_challenges_total{result},
 * trustflow_failed_logins_total, trustflow_high_risk_alerts_total, trustflow_denied_by_resource_total{resource}.
 */
@Component
public class TrustFlowMetrics {

    private final MeterRegistry registry;
    private final DistributionSummary riskScore;
    private final Counter failedLogins;
    private final Counter highRiskAlerts;

    public TrustFlowMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.riskScore = DistributionSummary.builder("trustflow.risk.score")
                .description("Distribution of calculated risk scores")
                .serviceLevelObjectives(10, 20, 30, 45, 60, 75, 100)
                .register(registry);
        this.failedLogins = Counter.builder("trustflow.failed.logins").description("Failed authentication attempts").register(registry);
        this.highRiskAlerts = Counter.builder("trustflow.high.risk.alerts").description("Alerts sent to the Lambda pipeline").register(registry);
        // Pre-register so series exist (at zero) before the first event; keeps alert rules and dashboards stable.
        for (String d : new String[]{"ALLOW", "MFA_REQUIRED", "DENY"}) decision(d, 0);
        for (String r : new String[]{"issued", "passed", "failed"}) mfa(r, 0);
    }

    public void decision(String decision) { decision(decision, 1); }

    private void decision(String decision, double amount) {
        registry.counter("trustflow.decisions", "decision", decision).increment(amount);
    }

    public void riskScore(int score) { riskScore.record(score); }

    public void mfa(String result) { mfa(result, 1); }

    private void mfa(String result, double amount) {
        registry.counter("trustflow.mfa.challenges", "result", result).increment(amount);
    }

    /** Feeds the "top denied resources" panel. Resource names are few, so label cardinality stays small. */
    public void deniedResource(String resource) {
        registry.counter("trustflow.denied.by.resource", "resource", resource).increment();
    }

    public void failedLogin() { failedLogins.increment(); }

    public void highRiskAlert() { highRiskAlerts.increment(); }
}
