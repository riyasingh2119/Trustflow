variable "region" {
  type    = string
  default = "ap-south-1"
}

variable "project" {
  type    = string
  default = "trustflow"
}

variable "admin_ip" {
  description = "Your public IP in CIDR form (for example 203.0.113.7/32). SSH, Jenkins, Grafana and Prometheus are open only to it."
  type        = string
  validation {
    condition     = can(cidrhost(var.admin_ip, 0)) && var.admin_ip != "0.0.0.0/0"
    error_message = "admin_ip must be a valid CIDR and must not be 0.0.0.0/0."
  }
}

variable "key_name" {
  description = "Name of an existing EC2 key pair for SSH."
  type        = string
}

variable "app_instance_type" {
  type    = string
  default = "t3.small"
}

variable "ci_instance_type" {
  description = "Jenkins and SonarQube need roughly 2 GB of RAM or more."
  type        = string
  default     = "t3.medium"
}

variable "monitoring_instance_type" {
  type    = string
  default = "t3.small"
}

variable "audit_bucket_name" {
  type    = string
  default = "trustflow-audit-exports"
}

variable "budget_limit_usd" {
  type    = number
  default = 10
}

variable "budget_email" {
  type = string
}

variable "enable_cloudtrail" {
  type    = bool
  default = true
}

variable "enable_config" {
  description = "AWS Config bills per recorded item, so it is off by default."
  type        = bool
  default     = false
}

variable "enable_nat_gateway" {
  description = "A NAT gateway bills every hour; keep false unless the private subnet needs internet access."
  type        = bool
  default     = false
}

variable "enable_db_instance" {
  description = "Optional PostgreSQL EC2 node in the private subnet (needs enable_nat_gateway to install packages)."
  type        = bool
  default     = false
}
