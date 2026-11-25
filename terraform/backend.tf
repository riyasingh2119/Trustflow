terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Remote state in S3 with a DynamoDB lock table. Both are created once by terraform/bootstrap
  # and are never destroyed together with the rest of the infrastructure.
  backend "s3" {
    bucket         = "trustflow-tf-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "trustflow-tf-lock"
    encrypt        = true
  }
}
