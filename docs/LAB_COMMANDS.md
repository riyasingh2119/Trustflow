# Lab command flows (INT378 practicals)

Capture each output with the date and your username visible.

## Lab 1: EC2, VPC, security groups, web server
Created by `terraform apply` (VPC, subnets, security groups, EC2). Then:
`ssh -i key.pem ubuntu@<app-ip>`, `systemctl status nginx`, open `http://<app-ip>` in a browser.
Show the VPC diagram and the security group rules (22 only from your IP, 5432 only from the app group).

## Lab 2: IAM
Terraform (`terraform/iam.tf`) creates groups `tf-admins`, `tf-developers`, custom policies, the EC2 instance role, the forced-MFA
policy and the `trustflow-operator` role. Evidence:
```bash
aws iam list-groups
aws iam get-policy-version --policy-arn <arn> --version-id v1
aws sts assume-role --role-arn <operator_role_arn> --role-session-name lab2 \
    --serial-number <mfa-device-arn> --token-code 123456
aws ec2 terminate-instances --instance-ids <id>     # as a tf-developers user: must be denied
```

## Lab 3: CloudFormation
```bash
cd cloudformation
aws cloudformation validate-template --template-body file://alert-stack.yaml
aws cloudformation create-stack --stack-name trustflow-alerts --template-body file://alert-stack.yaml \
  --parameters ParameterKey=AlertEmail,ParameterValue=you@example.com --capabilities CAPABILITY_IAM
aws cloudformation create-change-set --stack-name trustflow-alerts --change-set-name v2 \
  --template-body file://alert-stack-v2.yaml --capabilities CAPABILITY_IAM \
  --parameters ParameterKey=AlertEmail,UsePreviousValue=true
aws cloudformation describe-change-set --stack-name trustflow-alerts --change-set-name v2
aws cloudformation execute-change-set --stack-name trustflow-alerts --change-set-name v2
# deliberate failure: automatic rollback
aws cloudformation update-stack --stack-name trustflow-alerts --template-body file://alert-stack-v3-broken.yaml \
  --parameters ParameterKey=AlertEmail,UsePreviousValue=true --capabilities CAPABILITY_IAM
aws cloudformation describe-stack-events --stack-name trustflow-alerts --max-items 15
aws cloudformation describe-stacks --stack-name trustflow-alerts --query 'Stacks[0].Outputs'
```
Confirm the SNS subscription e-mail. Use the `AlertUrl` output as `ALERT_URL`.

## Lab 4: Docker
```bash
docker build -t riyasingh/trustflow-backend:1.0 backend
docker run -d --name tf-backend -p 8080:8080 --env-file .env riyasingh/trustflow-backend:1.0
docker ps && docker logs tf-backend
docker stop tf-backend && docker start tf-backend && docker rm -f tf-backend
docker login && docker push riyasingh/trustflow-backend:1.0
# on a second machine
docker pull riyasingh/trustflow-backend:1.0
```
(The backend needs a reachable PostgreSQL; the simplest is `docker compose up postgres` first.)

## Lab 5: Terraform
```bash
terraform init && terraform validate
terraform plan -out tf.plan && terraform apply tf.plan
# change app_instance_type in terraform.tfvars, then
terraform plan && terraform apply
terraform state list
terraform destroy
```

## Lab 6 and 7: Jenkins and DevSecOps
See `jenkins/SETUP.md`. Capture: webhook delivery in GitHub, stage view, the deployed version changing, SonarQube, Dependency-Check
and Trivy reports, one failed build (quality gate or CRITICAL finding) and the fixed build.

## Lab 8 and 9: Prometheus, Node Exporter, Grafana
```bash
sudo ./scripts/install-node-exporter.sh
# Prometheus: Status > Targets (all UP). Trigger alerts:
sudo systemctl stop node_exporter                 # InstanceDown after 1 minute
stress-ng --cpu 0 --timeout 400s                  # HighCpu after 5 minutes  (sudo apt install stress-ng)
```
Grafana dashboards are provisioned from `monitoring/grafana/dashboards`; export JSON from the UI for the report.

## Lab 10: Lambda, API Gateway, CloudWatch
```bash
curl -X POST "$ALERT_URL" -H 'Content-Type: application/json' -H "X-TrustFlow-Key: $ALERT_KEY" \
  -d '{"user":"dev","resource":"Prod Customer DB","score":72,"decision":"DENY","reason":"test"}'
aws logs tail /aws/lambda/trustflow-notify --since 10m
```
Show the Lambda test event, the CloudWatch log stream, the invocation metrics and the received e-mail.

## Lab 11: Cost Explorer and Budgets
Tags `project=trustflow` are applied by Terraform; activate the tag in Billing > Cost allocation tags. Capture the budget alert,
Cost Explorer grouped by service, and a right-sizing note from CloudWatch CPU graphs.
