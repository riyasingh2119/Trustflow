output "app_public_ip" {
  value = aws_instance.app.public_ip
}

output "ci_public_ip" {
  value = aws_instance.ci.public_ip
}

output "monitoring_public_ip" {
  value = aws_instance.monitoring.public_ip
}

output "vpc_id" {
  value = aws_vpc.main.id
}

output "audit_bucket" {
  value = aws_s3_bucket.audit.bucket
}

output "operator_role_arn" {
  value = aws_iam_role.operator.arn
}
