# Deployment guide (AWS, student budget)

Always check current prices and free-tier eligibility first, and run `terraform destroy` after the demonstration.

## 1. One-time prerequisites
- AWS account with MFA on the root user; budget alerts are created by Terraform (`budget_email`).
- An EC2 key pair; your public IP (`curl -s https://checkip.amazonaws.com`).
- Docker Hub account and access token. Terraform, AWS CLI v2.

## 2. State backend (once)
```bash
cd terraform/bootstrap && terraform init && terraform apply
```

## 3. Infrastructure
```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars   # edit
terraform init && terraform validate && terraform plan -out tf.plan && terraform apply tf.plan
```
Outputs give the public IPs of the app, CI and monitoring nodes.

## 4. Alert pipeline (CloudFormation)
See `docs/LAB_COMMANDS.md`, section CloudFormation. Copy the `AlertUrl` output into `ALERT_URL`
(Kubernetes secret `trustflow-secrets` or the systemd env file) and confirm the SNS e-mail subscription.

## 5a. Application on k3s (matches the Kubernetes design)
```bash
ssh -i key.pem ubuntu@<app-ip>
sudo ADMIN_IP=<your-ip> ./scripts/hardening.sh
curl -sfL https://get.k3s.io | sh -          # single-node cluster, Traefik ingress and metrics-server included
sudo kubectl apply -f k8s/00-namespace.yaml -f k8s/10-configmap.yaml
# create secrets as described in k8s/11-secret.example.yaml, then:
sudo kubectl apply -f k8s/20-postgres.yaml -f k8s/30-backend.yaml -f k8s/40-frontend.yaml \
                   -f k8s/50-ingress.yaml -f k8s/60-rbac.yaml -f k8s/70-networkpolicy.yaml -f k8s/80-hpa.yaml
```
Add `<app-ip> trustflow.local` to your hosts file, or change the host in `k8s/50-ingress.yaml`.
Set `CORS_ORIGIN` in `k8s/10-configmap.yaml` to the URL you use.

## 5b. Application as a plain Linux service (CSE493 demonstration)
Install Java 17 and Nginx, create `/etc/trustflow/trustflow.env` from `scripts/etc/trustflow.env.example`, copy
`scripts/systemd/trustflow-backend.service`, run `sudo ./scripts/deploy.sh <image:tag>`, build the frontend and copy `dist/` to
`/var/www/trustflow`, and install `scripts/etc/nginx-trustflow.conf`. PostgreSQL can be installed with apt on the same node.

## 6. CI/CD
Follow `jenkins/SETUP.md`. A push to GitHub then builds, tests, scans, pushes images and updates the Deployments.

## 7. Monitoring
Run `scripts/install-node-exporter.sh` on every server. On the monitoring node run Prometheus, Alertmanager and Grafana with
the files in `monitoring/` (the compose file's `monitoring` profile is the quickest way). Replace the Prometheus targets with the
private IPs of your nodes (backend `:8080/actuator/prometheus`, node exporters `:9100`). Restrict `/actuator` to the internal network.

## 8. Clean-up
`terraform destroy` for the main stack (never the bootstrap stack), delete the CloudFormation stack, stop EC2 instances between sessions.
