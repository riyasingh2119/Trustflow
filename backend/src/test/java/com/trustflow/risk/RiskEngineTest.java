package com.trustflow.risk;

import com.trustflow.context.DeviceStatus;
import com.trustflow.resource.Sensitivity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** TDD: these tests were written first (red), then RiskEngine was implemented until they passed (green). */
class RiskEngineTest {

    private final RiskEngine engine = new RiskEngine(RiskParams.defaults());

    @ParameterizedTest(name = "{0}")
    @CsvSource({
        "Developer on managed laptop reads LOW dev API at 11:00,   MANAGED_COMPLIANT, LOW,  11, 0,  5, ALLOW",
        "Unknown device reads HIGH prod DB at 14:00,               UNKNOWN,           HIGH, 14, 0, 45, MFA_REQUIRED",
        "Non-compliant HIGH 19:00 one failed login,                NON_COMPLIANT,     HIGH, 19, 1, 72, DENY",
        "Managed device HIGH resource at 23:00,                    MANAGED_COMPLIANT, HIGH, 23, 0, 45, MFA_REQUIRED"
    })
    void workedExamplesFromTheDesign(String name, DeviceStatus device, Sensitivity s, int hour, int failed,
                                     int score, Decision decision) {
        RiskResult r = engine.evaluate(device, s, hour, failed);
        assertEquals(score, r.score());
        assertEquals(decision, r.decision());
    }

    @ParameterizedTest
    @CsvSource({"9,0", "17,0", "18,7", "21,7", "22,15", "23,15", "0,15", "5,15", "6,7", "8,7"})
    void timeRiskBands(int hour, int expected) {
        assertEquals(expected, engine.evaluate(DeviceStatus.MANAGED_COMPLIANT, Sensitivity.LOW, hour, 0).factors().time());
    }

    @Test
    void authenticationRiskIsFivePerFailureCappedAtTwentyFive() {
        assertEquals(10, engine.evaluate(DeviceStatus.MANAGED_COMPLIANT, Sensitivity.LOW, 10, 2).factors().auth());
        assertEquals(25, engine.evaluate(DeviceStatus.MANAGED_COMPLIANT, Sensitivity.LOW, 10, 9).factors().auth());
    }

    @Test
    void maximumScoreIsOneHundred() {
        assertEquals(100, engine.evaluate(DeviceStatus.NON_COMPLIANT, Sensitivity.HIGH, 3, 50).score());
    }

    @Test
    void thresholdBoundaries30And31() {
        assertEquals(Decision.ALLOW, engine.decide(30));
        assertEquals(Decision.MFA_REQUIRED, engine.decide(31));
    }

    @Test
    void thresholdBoundaries60And61() {
        assertEquals(Decision.MFA_REQUIRED, engine.decide(60));
        assertEquals(Decision.DENY, engine.decide(61));
    }

    @Test
    void thresholdsComeFromConfiguration() {
        // The alternative configuration used in the A/B experiment: MFA band 26-55
        RiskParams strict = RiskParams.fromMap(Map.of("threshold.allow_max", 25, "threshold.mfa_max", 55));
        RiskEngine strictEngine = new RiskEngine(strict);
        assertEquals(Decision.MFA_REQUIRED, strictEngine.decide(26));
        assertEquals(Decision.DENY, strictEngine.decide(56));
    }

    @Test
    void reasonExplainsEveryContributingFactor() {
        RiskResult r = engine.evaluate(DeviceStatus.UNKNOWN, Sensitivity.HIGH, 14, 0);
        assertEquals("Device UNKNOWN (+15), resource HIGH (+30), score 45 in MFA band", r.reason());
    }
}
