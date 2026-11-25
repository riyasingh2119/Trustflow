# Security design

## Application
- Passwords stored with BCrypt. JWT (HS256) with a secret of at least 32 bytes from the environment, 15 minute expiry. A
  tampered, expired or wrongly signed token is rejected. Challenge and MFA tokens are separate token types.
- MFA is a simulated TOTP (HMAC-SHA1 time counter, six digits, current and previous 30 second step). MFA tokens are bound to the
  user, resource and action and are single use.
- Input validation on all DTOs, JPA parameterised queries, CORS limited to the frontend origin, security headers in Nginx.
- Admin endpoints need ROLE_ADMIN; `/api/audit` needs ADMIN or AUDITOR. Audit records are append-only (`@Immutable`, no update
  or delete endpoint).
- Login timing is equalised for unknown usernames, and failed logins feed the risk score.
- `/actuator` is not routed by Nginx or the ingress; Prometheus reaches it on the internal network only.

## Secrets and keys
| Where | How |
|-------|-----|
| Source code | no secrets committed; `.env` is git-ignored |
| Jenkins | credentials store (Docker Hub, kubeconfig, Sonar token), used in single-quoted shell steps |
| Kubernetes | Secret objects for DB password, JWT key, alert URL and key; ConfigMap only for non-sensitive settings |
| AWS | instance roles instead of access keys; optionally Secrets Manager or KMS for the JWT key |

## Cloud and cluster
IAM least privilege with MFA and a role to assume (`terraform/iam.tf`), CloudTrail, optional AWS Config rules (no public S3, no
open SSH). Kubernetes: RBAC for the deployer, NetworkPolicy (default deny, database reachable from the backend only),
non-root containers, read-only root filesystem for the backend, dropped capabilities, resource limits, image scanning before deploy.

## 12-factor
| Factor | TrustFlow |
|--------|-----------|
| Codebase | one Git repository, many deploys |
| Dependencies | `pom.xml`, `package.json` |
| Config | environment variables, ConfigMap, Secret |
| Backing services | PostgreSQL and the alert endpoint attached by URL |
| Build, release, run | Jenkins builds the image, Kubernetes releases, the container runs |
| Processes | stateless backend (JWT, signed challenges, no server sessions) |
| Port binding | embedded server on 8080 |
| Concurrency | replicas and HPA |
| Disposability | fast start, graceful shutdown, probes |
| Dev and prod parity | the same image from compose to Kubernetes |
| Logs | stdout, collected by journald or Kubernetes |
| Admin processes | Flyway migrations and one-off Jobs |

## Known limitations
Rule-based risk engine, simulated device attestation and MFA, single-node cluster, demo-mode shortcuts, the alert endpoint is
protected only by a shared secret, and no enterprise-scale load testing. No claim of production-grade threat intelligence or
incident response is made.

Future work: ML anomaly detection, real device posture agents, OAuth 2.0 / OpenID Connect / SAML, policy-as-code (Open Policy
Agent), service mesh with mutual TLS, Kafka event streaming, managed Kubernetes (EKS), automated incident response.
