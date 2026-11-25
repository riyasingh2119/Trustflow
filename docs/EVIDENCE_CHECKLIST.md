# Evidence checklist (report and viva)

- [ ] Screenshots for every lab in `docs/SYLLABUS_MAPPING.md`, each with the date and your username visible.
- [ ] Exported files: Terraform state listing, CloudFormation change set, Jenkins build console, SonarQube, Dependency-Check and Trivy
      reports, Grafana dashboard JSON, Prometheus rules.
- [ ] Git history showing the TDD order and sprint commits; GitHub Projects board screenshots.
- [ ] Tables: Terraform vs CloudFormation, AWS vs Azure, 12-factor compliance, risk worked examples, A/B experiment.
- [ ] Case studies: Netflix, Amazon, Uber (see `docs/THEORY_NOTES.md`).
- [ ] Linux: `scripts/hardening.sh` before and after output, `systemctl status` and `journalctl` of the backend and Node Exporter,
      `ufw status`, `ss -tlnp`, `df -h` and `du` checks, and `top`/`htop`/`vmstat` during the load test compared with Grafana.

## Report outline
Abstract, Introduction, Problem, Objectives, Existing vs Proposed, Architecture, Design (database and API), Risk engine,
DevOps and DevSecOps, Cloud deployment, IaC, Kubernetes, Monitoring, Testing, Results and screenshots, Cost analysis,
Limitations, Future work, References.

## Cost management plan
Tag every resource `project=trustflow` and activate the tag in billing; keep the budget alerts at 50, 80 and 100 percent; use the
smallest instances that work and single-node k3s instead of EKS; avoid NAT gateways and idle Elastic IPs; stop instances after each
session and run `terraform destroy` after the demonstration; review Cost Explorer by service and write one right-sizing
recommendation with the expected saving.
