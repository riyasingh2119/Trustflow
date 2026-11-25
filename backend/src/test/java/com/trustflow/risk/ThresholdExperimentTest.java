package com.trustflow.risk;

import com.trustflow.context.DeviceStatus;
import com.trustflow.resource.Sensitivity;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A/B experiment (INT378 unit III): the same simulated traffic is scored under two threshold sets.
 *   A (baseline): ALLOW 0-30, MFA 31-60, DENY 61-100
 *   B (strict):   ALLOW 0-25, MFA 26-55, DENY 56-100
 * The printed table is the evidence for the report.
 */
class ThresholdExperimentTest {

    private static final int REQUESTS = 10_000;

    private Map<Decision, Integer> run(RiskParams params) {
        RiskEngine engine = new RiskEngine(params);
        Random rnd = new Random(42);                       // fixed seed: identical traffic for A and B
        Map<Decision, Integer> counts = new EnumMap<>(Decision.class);
        for (Decision d : Decision.values()) counts.put(d, 0);
        for (int i = 0; i < REQUESTS; i++) {
            DeviceStatus device = pick(rnd, DeviceStatus.MANAGED_COMPLIANT, 70, DeviceStatus.UNKNOWN, 20, DeviceStatus.NON_COMPLIANT, 10);
            Sensitivity s = pick(rnd, Sensitivity.LOW, 40, Sensitivity.MEDIUM, 35, Sensitivity.HIGH, 25);
            int hour = rnd.nextInt(100) < 75 ? 9 + rnd.nextInt(9) : rnd.nextInt(24);
            int failures = rnd.nextInt(100) < 85 ? 0 : 1 + rnd.nextInt(4);
            counts.merge(engine.evaluate(device, s, hour, failures).decision(), 1, Integer::sum);
        }
        return counts;
    }

    private static <T> T pick(Random rnd, T a, int wa, T b, int wb, T c, int wc) {
        int r = rnd.nextInt(wa + wb + wc);
        return r < wa ? a : r < wa + wb ? b : c;
    }

    @Test
    void strictThresholdsChallengeAndDenyMoreOften() {
        Map<Decision, Integer> a = run(RiskParams.defaults());
        Map<Decision, Integer> b = run(RiskParams.fromMap(Map.of("threshold.allow_max", 25, "threshold.mfa_max", 55)));

        System.out.printf("%nA/B threshold experiment over %d simulated requests%n", REQUESTS);
        System.out.printf("%-22s %8s %8s %8s%n", "configuration", "ALLOW", "MFA", "DENY");
        System.out.printf("%-22s %7.1f%% %7.1f%% %7.1f%%%n", "A  30/60 (baseline)", pct(a, Decision.ALLOW), pct(a, Decision.MFA_REQUIRED), pct(a, Decision.DENY));
        System.out.printf("%-22s %7.1f%% %7.1f%% %7.1f%%%n%n", "B  25/55 (strict)", pct(b, Decision.ALLOW), pct(b, Decision.MFA_REQUIRED), pct(b, Decision.DENY));

        assertTrue(b.get(Decision.ALLOW) < a.get(Decision.ALLOW), "strict set must allow fewer requests outright");
        assertTrue(b.get(Decision.DENY) >= a.get(Decision.DENY), "strict set must deny at least as many requests");
    }

    private static double pct(Map<Decision, Integer> m, Decision d) {
        return 100.0 * m.get(d) / REQUESTS;
    }
}
