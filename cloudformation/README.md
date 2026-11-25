# Alert stack (lab 3 and lab 10)

| File | Purpose |
|------|---------|
| `alert-stack.yaml` | SNS topic, Lambda, API Gateway HTTP API, IAM role. Lambda code is generated from `lambda/notify.py`. |
| `alert-stack-v2.yaml` | Update for the change-set demo: timeout, memory size and a topic display name. |
| `alert-stack-v3-broken.yaml` | Deliberately invalid runtime. The update fails and CloudFormation rolls back automatically. |

If you edit `lambda/notify.py`, regenerate the inline code with `scripts/build-alert-stack.py`.

Command flow: see `docs/LAB_COMMANDS.md` (CloudFormation section).
