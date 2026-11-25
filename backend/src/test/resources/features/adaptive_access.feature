Feature: Adaptive access decision

  Scenario: Managed device reading a low-sensitivity resource
    Given a developer on a managed device
    And a LOW sensitivity development API
    When the developer requests READ access during business hours
    Then the decision is ALLOW

  Scenario: Non-compliant device reaching a production database
    Given a developer on a non-compliant device
    And a HIGH sensitivity production database
    And one failed login in the last 15 minutes
    When the developer requests READ access at 19:00
    Then the risk score is 72
    And the decision is DENY
    And a high-risk alert is published

  Scenario: Unknown device requires MFA
    Given a developer on an unknown device
    And a HIGH sensitivity production database
    When the developer requests READ access at 14:00
    Then the risk score is 45
    And the decision is MFA_REQUIRED
