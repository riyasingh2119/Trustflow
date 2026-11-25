# Agile plan, TDD, BDD and the A/B experiment

## Sprints (one week each, adjust to your deadline)
| Sprint | Goal | Deliverables |
|--------|------|--------------|
| 1 | Foundation | repository, Spring Boot skeleton, PostgreSQL and Flyway, users and roles, JWT login, Docker and compose, auth tests |
| 2 | Authorization core | resources, policies, PolicyEvaluator, audit logging, admin APIs |
| 3 | Risk and gateway | RiskEngine (TDD), context collector, gateway, MFA flow, Cucumber scenarios |
| 4 | Frontend | login, access console with decision display, admin screens, audit viewer |
| 5 | CI/CD and DevSecOps | Terraform, Jenkins with webhook, Sonar, Dependency-Check, Trivy, Docker Hub |
| 6 | Kubernetes and observability | manifests, RBAC, NetworkPolicy, HPA, Prometheus, Node Exporter, Alertmanager, Grafana |
| 7 | Serverless and governance | CloudFormation alert stack, API Gateway, Lambda, CloudWatch, IAM, CloudTrail, Budgets |
| 8 | Hardening and report | Linux hardening, load test, A/B experiment, screenshots, report and presentation |

Track user stories and story points on a GitHub Projects board and write a short retrospective each week.
Record the SonarQube technical-debt ratio at the end of each sprint and show the trend.

## TDD
`RiskEngineTest` was designed to be written first: red (tests only), green (implementation), refactor. Make the commit history show
that order (`test: risk engine scoring`, then `feat: risk engine`, then `refactor: ...`).

## BDD
`backend/src/test/resources/features/adaptive_access.feature` with steps in `AdaptiveAccessSteps`. The HTML report is written to
`backend/target/cucumber-report.html` and archived by Jenkins.

## A/B experiment
`ThresholdExperimentTest` scores 10,000 identical simulated requests under two threshold sets and prints the share of each
decision. Run `mvn -Dtest=ThresholdExperimentTest test` and copy the table into the report. Observed with the shipped seed of
the random generator:

| Configuration | ALLOW | MFA | DENY |
|---------------|-------|-----|------|
| A baseline 30/60 | 74.6% | 24.1% | 1.3% |
| B strict 25/55 | 53.2% | 42.6% | 4.2% |

The traffic mix is an assumption (70% managed devices, 75% business hours, 15% of requests after failed logins), so discuss
the result as a comparison of configurations, not as a prediction of real-world rates.
