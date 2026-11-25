# End-to-end demo script

1. Show the Git push that triggers Jenkins; walk through the pipeline stages and reports.
2. Show the running system on the EC2 public address; Grafana and Prometheus dashboards open in other tabs.
3. Sign in as `dev`, laptop-managed-01, Dev Orders API, 11:00: **ALLOW, score 5**.
4. Switch the device to phone-byod-02 and request Prod Customer DB at 11:00: **MFA_REQUIRED, score 45**. Use the demo authenticator,
   verify, see the request allowed. Show the audit row with the factor breakdown (as `auditor` or `admin`).
5. Make one wrong-password attempt for `dev`, sign in again, use old-pc-03 on Prod Customer DB at 19:00: **DENY, score 72**. Show
   the audit record, the Lambda log in CloudWatch and the alert e-mail.
6. As `analyst` request WRITE on a resource: **DENY, NO_POLICY**.
7. As `dev` ask for temporary access to a resource you lack; as `admin` approve it for 1 hour and show the policy (Administration >
   Policies) with its end time.
8. Administration > Risk rules: change a threshold and show the effect on a new request within 30 seconds.
9. Run `scripts/loadtest.sh`; show `kubectl get hpa -n trustflow -w` scaling from 2 towards 6 and the Grafana security dashboard.
10. Stop Node Exporter and show the InstanceDown alert firing in Prometheus and the Alertmanager e-mail.
11. Show the Terraform plan or destroy output, the CloudFormation stack, the Budget alert and the Linux hardening before and after output.
