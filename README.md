# TrustFlow

A cloud-native zero-trust access management and dynamic risk-based authorization platform (prototype).

For every request to a protected resource TrustFlow decides **ALLOW**, **MFA_REQUIRED** or **DENY**. The decision combines
who is asking (JWT identity and role), what they ask for (resource, action, sensitivity) and the context (device status,
time of access, recent failed logins). Every decision is written to an audit trail with a plain-language reason, and a
high-risk denial triggers a serverless alert (API Gateway, Lambda, SNS e-mail).





| URL | What |
|-----|------|
| http://localhost:8081 | Web app (React, served by Nginx, `/api` proxied) |
| http://localhost:8080/actuator/health | Backend health |
| http://localhost:8080/actuator/prometheus | Metrics |

Sign in with a demo account (password `Passw0rd!` for all):

| User | Role | Notes |
|------|------|-------|
| `dev` | DEVELOPER | has three devices: managed laptop, unknown phone, non-compliant old PC |
| `analyst` | ANALYST | read access to analytics and the production database |
| `auditor` | AUDITOR | read-only access to the audit trail |
| `admin` | ADMIN | users, resources, policies, risk rules, approvals |

Monitoring stack: `docker compose --profile monitoring up -d` then Prometheus `:9090`, Grafana `:3000`, Alertmanager `:9093`.

> The seeded demo passwords and `DEMO_MODE=true` exist for demonstrations. For any real deployment set
> `DEMO_MODE=false` (turns off the simulated-hour override and the demo authenticator), delete or change the seed users, and use
> real secrets.

### Try the four worked examples

In the web app, sign in as `dev` and choose:

| Resource | Device | Time | Expected |
|----------|--------|------|----------|
| Dev Orders API | laptop-managed-01 | 11:00 | ALLOW, score 5 |
| Prod Customer DB | phone-byod-02 (unknown) | 11:00 | MFA_REQUIRED, score 45 (use "Use demo authenticator", then Verify) |
| Prod Customer DB | old-pc-03 (non-compliant) after one failed login | 19:00 | DENY, score 72 |
| Prod Customer DB | laptop-managed-01 | 23:00 | MFA_REQUIRED, score 45 |

For the third row, first sign out and make one wrong-password attempt for `dev`, then sign in correctly.

### Run the tests

```bash
cd backend
mvn test          # unit (JUnit 5), BDD (Cucumber), API/integration (Testcontainers, skipped if Docker is absent)
```

`ThresholdExperimentTest` prints the A/B comparison of two threshold sets.

## Repository layout

```
backend/          Spring Boot 3 (Java 17) modular monolith: auth, user, resource, policy, risk, context, gateway, audit, alert
frontend/         React 18 + Vite app, Dockerfile, Nginx template
k8s/              namespace, config, secrets template, postgres, backend, frontend, ingress, RBAC, NetworkPolicy, HPA
terraform/        VPC, subnets, security groups, EC2, S3, IAM, budgets, CloudTrail, optional Config (+ bootstrap/ for remote state)
cloudformation/   alert-stack.yaml (+ v2 change set, v3 deliberate failure)
lambda/           notify.py (+ unit test)
monitoring/       Prometheus, alert rules, Alertmanager, Grafana provisioning and dashboards
jenkins/          Jenkinsfile (+ SETUP.md)
scripts/          hardening.sh, deploy.sh, loadtest.sh, Node Exporter installer, systemd units, host Nginx config
docs/             architecture, deployment, lab commands, syllabus mapping, demo script, evidence checklist, theory notes
docker-compose.yml
```

## How a request is decided

`POST /api/gateway/access` (see `gateway/AccessDecisionFilter`):

1. JWT validated, identity established (401 otherwise).
2. User disabled: DENY.
3. No active, in-validity policy for the role (or a temporary per-user grant): DENY with reason `NO_POLICY`.
4. Context collected: device status from the `X-Device-Id` header and device registry, hour of day, failed logins in the last 15 minutes.
5. Risk score = device (0-30) + resource (5/15/30) + time (0/7/15) + authentication (5 per failure, cap 25).
6. 0-30 ALLOW, 31-60 MFA_REQUIRED, 61-100 DENY. All weights and thresholds are rows in `risk_config`, editable by the admin.
7. MFA_REQUIRED returns a challenge id. The client submits the one-time code to `/api/auth/mfa/verify`, receives a single-use
   token and retries with `X-MFA-Token`; that one request is then allowed.
8. The outcome is stored in `audit_events` (append-only) with the factor breakdown and reason; DENY also publishes the alert.

## Scope decisions made in the design

Modular monolith, Spring events instead of Kafka, deterministic rule-based risk engine (no ML claims), simulated TOTP and
simulated device attestation (registry plus request header). These are stated limitations, see `docs/`.

## Small extensions beyond the design document

- `policies.user_id` (nullable): approved temporary-access requests create a time-bound policy for that one user.
- `used_mfa_tokens` table: makes the MFA token single-use across replicas while keeping the backend stateless.
- `GET /api/auth/me`, `GET /api/auth/mfa/demo-code` (demo mode), `GET /api/audit/summary`, `GET /api/admin/roles`.
- Optional shared secret (`ALERT_KEY`, header `X-TrustFlow-Key`) between the backend and the alert Lambda.

## Verification status

What was run while generating this repository: the risk engine against all worked examples and threshold boundaries, the A/B
experiment, the Lambda unit tests, the frontend production build, syntax checks of every Java file, YAML parsing of the
Kubernetes, CloudFormation and monitoring files, `tofu fmt` on the Terraform and `bash -n` on the scripts.

Not run here (no Maven Central, Docker or AWS access): the full `mvn test`, container builds, `terraform validate/plan` and
any AWS deployment. Run those first on your machine; expect to fix small version or naming issues, as with any new project.

See `docs/DEPLOYMENT.md` for the AWS rollout and `docs/EVIDENCE_CHECKLIST.md` for what to capture for the viva.
