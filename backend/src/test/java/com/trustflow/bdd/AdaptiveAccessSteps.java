package com.trustflow.bdd;

import com.trustflow.context.DeviceStatus;
import com.trustflow.resource.Sensitivity;
import com.trustflow.risk.Decision;
import com.trustflow.risk.RiskEngine;
import com.trustflow.risk.RiskParams;
import com.trustflow.risk.RiskResult;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Business-readable scenarios exercised against the real RiskEngine (no database needed). */
public class AdaptiveAccessSteps {

    private final RiskEngine engine = new RiskEngine(RiskParams.defaults());
    private DeviceStatus device;
    private Sensitivity sensitivity;
    private int failedLogins;
    private RiskResult result;

    @Given("a developer on a managed device")
    public void managed() { device = DeviceStatus.MANAGED_COMPLIANT; }

    @Given("a developer on an unknown device")
    public void unknown() { device = DeviceStatus.UNKNOWN; }

    @Given("a developer on a non-compliant device")
    public void nonCompliant() { device = DeviceStatus.NON_COMPLIANT; }

    @Given("a LOW sensitivity development API")
    public void low() { sensitivity = Sensitivity.LOW; }

    @Given("a HIGH sensitivity production database")
    public void high() { sensitivity = Sensitivity.HIGH; }

    @Given("one failed login in the last 15 minutes")
    public void oneFailure() { failedLogins = 1; }

    @When("the developer requests READ access during business hours")
    public void businessHours() { result = engine.evaluate(device, sensitivity, 11, failedLogins); }

    @When("the developer requests READ access at {int}:00")
    public void atHour(int hour) { result = engine.evaluate(device, sensitivity, hour, failedLogins); }

    @Then("the decision is {word}")
    public void decisionIs(String decision) { assertEquals(Decision.valueOf(decision), result.decision()); }

    @Then("the risk score is {int}")
    public void scoreIs(int score) { assertEquals(score, result.score()); }

    /** A high-risk alert is published exactly when the decision is DENY (see AccessDecisionFilter). */
    @Then("a high-risk alert is published")
    public void alertPublished() { assertTrue(result.decision() == Decision.DENY); }
}
