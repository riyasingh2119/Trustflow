# INT378 syllabus mapping

## Practicals

| Lab | Syllabus requirement | Where in this repository |
|-----|----------------------|--------------------------|
| 1 | EC2, VPC, security groups, SSH, web server | `terraform/main.tf`, `scripts/etc/nginx-trustflow.conf` |
| 2 | IAM users, groups, roles, custom policies, MFA, least privilege | `terraform/iam.tf` |
| 3 | CloudFormation, change sets, rollback, comparison with Terraform | `cloudformation/`, `docs/THEORY_NOTES.md` |
| 4 | Docker image, run, ports, Docker Hub, lifecycle | `backend/Dockerfile`, `frontend/Dockerfile`, `docker-compose.yml` |
| 5 | Terraform init, plan, apply, modify, destroy, state | `terraform/` |
| 6 | Jenkins and GitHub webhook CI/CD | `jenkins/Jenkinsfile` |
| 7 | DevSecOps in Jenkins | Sonar, Dependency-Check and Trivy stages in the Jenkinsfile |
| 8 | Prometheus, Node Exporter, alert rules | `monitoring/prometheus.yml`, `alert-rules.yml`, `scripts/install-node-exporter.sh` |
| 9 | Grafana dashboards | `monitoring/grafana/` |
| 10 | Lambda, API Gateway, CloudWatch | `lambda/notify.py`, `cloudformation/alert-stack.yaml` |
| 11 | Cost Explorer, Budgets, optimisation | `terraform/governance.tf`, `docs/DEPLOYMENT.md` |

## Theory units

| Unit | Topic | Where |
|------|-------|-------|
| I | AWS and Azure services, Docker architecture, case studies | `docs/THEORY_NOTES.md` |
| II | IaC, Terraform vs CloudFormation, trends | `docs/THEORY_NOTES.md` |
| III | Agile, TDD, BDD, CI/CD, A/B testing, technical debt | `docs/AGILE_AND_TESTING.md` |
| IV | Prometheus, Grafana, alerting, scaling | `monitoring/`, `k8s/80-hpa.yaml`, `scripts/loadtest.sh` |
| V | DevSecOps, IAM, Kubernetes and container security, secrets | `jenkins/`, `k8s/60-rbac.yaml`, `k8s/70-networkpolicy.yaml`, `docs/SECURITY.md` |
| VI | Cloud native, 12-factor, serverless, cost, governance | `docs/SECURITY.md` (12-factor), `lambda/`, `terraform/governance.tf` |

## CSE493 Linux system administration
`scripts/hardening.sh` (users, SSH, ufw, fail2ban, unattended upgrades, sysctl, permissions, logrotate),
`scripts/systemd/` (services with restart policy and an unprivileged user), `scripts/deploy.sh`,
`scripts/etc/nginx-trustflow.conf`, and the monitoring tools listed in the viva evidence checklist.
