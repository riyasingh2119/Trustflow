# Theory notes for the report (units I and II)

Verify the case-study facts against the sources you cite in the References chapter before submitting.

## AWS and Azure service equivalents

| Purpose | AWS | Azure |
|---------|-----|-------|
| Virtual machines | EC2 | Virtual Machines |
| Object storage | S3 | Blob Storage |
| Identity and access | IAM | Microsoft Entra ID and Azure RBAC |
| Serverless functions | Lambda | Azure Functions |
| Monitoring and logs | CloudWatch | Azure Monitor |
| Private network | VPC | Virtual Network |
| Infrastructure as code | CloudFormation | ARM templates and Bicep |
| Container orchestration | EKS | AKS |
| Messaging and notifications | SNS | Event Grid and Notification Hubs |
| Cost tools | Cost Explorer, Budgets | Cost Management |

## Docker architecture
The Docker client talks to the Docker daemon (`dockerd`), which manages images, containers, networks and volumes and delegates
container execution to containerd and runc. Images are built from layered filesystems, stored in a registry (Docker Hub) and run as
isolated processes using Linux namespaces and cgroups. Lifecycle: create, start, stop, restart, remove. In TrustFlow, multi-stage
builds keep build tools out of the runtime image and the containers run as non-root users.

## Terraform vs CloudFormation

| Aspect | Terraform | CloudFormation |
|--------|-----------|----------------|
| Language | HCL (declarative) | YAML or JSON |
| Scope | many providers and clouds | AWS only |
| State | explicit state file (remote backend, locking) | managed by AWS in the stack |
| Preview | `terraform plan` | change sets |
| Rollback | re-apply previous code, no automatic rollback | automatic rollback on failed create or update |
| Reuse | modules | nested stacks, modules, macros |
| New AWS features | needs a provider release | often available first |
| In TrustFlow | network, compute, storage, IAM, governance | alert pipeline |

## Trends
Pulumi (general-purpose languages for IaC), OpenTofu (community fork of Terraform), AWS CDK (generates CloudFormation from code),
and policy-as-code (Open Policy Agent, Sentinel, Checkov) that checks plans before they are applied.

## Case studies (one paragraph each)
- **Netflix.** Netflix moved from its own data centres to AWS and decomposed its monolith into many microservices, a migration it
  has described publicly as completing in the mid-2010s. Lessons for scalability: design for failure (it popularised chaos
  engineering with Chaos Monkey), keep services stateless and replicated, and use elastic capacity for peaks.
- **Amazon.** Amazon's retail platform moved from a monolith to service-oriented architecture with small teams owning services behind
  well-defined APIs, an approach that also led to the creation of AWS. Lessons: clear service boundaries and ownership allow teams
  to deploy independently and scale each service on its own.
- **Uber.** Uber grew from a monolith to a very large number of microservices and later described grouping them into domains to
  reduce complexity. Lessons: microservices speed up independent delivery but add operational and dependency complexity, so
  observability and clear domain boundaries matter. TrustFlow's modular monolith reflects the opposite trade-off for a small project.
