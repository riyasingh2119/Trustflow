# ---------------- EC2 instance role (no access keys on servers) ----------------
data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2" {
  name               = "trustflow-ec2-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

data "aws_iam_policy_document" "ec2_permissions" {
  statement {
    sid     = "AuditBucketReadWrite"
    actions = ["s3:PutObject", "s3:GetObject", "s3:ListBucket"]
    resources = [
      aws_s3_bucket.audit.arn,
      "${aws_s3_bucket.audit.arn}/*",
    ]
  }

  statement {
    sid = "CloudWatchLogsAndMetrics"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
      "cloudwatch:PutMetricData",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "ec2" {
  name   = "trustflow-ec2-least-privilege"
  policy = data.aws_iam_policy_document.ec2_permissions.json
}

resource "aws_iam_role_policy_attachment" "ec2" {
  role       = aws_iam_role.ec2.name
  policy_arn = aws_iam_policy.ec2.arn
}

resource "aws_iam_instance_profile" "ec2" {
  name = "trustflow-ec2-profile"
  role = aws_iam_role.ec2.name
}

# ---------------- human access: groups, custom policies, MFA, role assumption ----------------
resource "aws_iam_group" "admins" {
  name = "tf-admins"
}

resource "aws_iam_group" "developers" {
  name = "tf-developers"
}

# Deny everything except MFA self-service until the user has signed in with MFA
data "aws_iam_policy_document" "force_mfa" {
  statement {
    sid    = "AllowManageOwnMfa"
    effect = "Allow"
    actions = [
      "iam:ListVirtualMFADevices",
      "iam:CreateVirtualMFADevice",
      "iam:EnableMFADevice",
      "iam:ResyncMFADevice",
      "iam:ListMFADevices",
      "iam:GetUser",
      "iam:ChangePassword",
    ]
    resources = ["*"]
  }

  statement {
    sid         = "DenyAllWithoutMfa"
    effect      = "Deny"
    not_actions = ["iam:*MFA*", "iam:GetUser", "iam:ListVirtualMFADevices", "iam:ChangePassword", "sts:GetSessionToken"]
    resources   = ["*"]

    condition {
      test     = "BoolIfExists"
      variable = "aws:MultiFactorAuthPresent"
      values   = ["false"]
    }
  }
}

resource "aws_iam_policy" "force_mfa" {
  name   = "trustflow-force-mfa"
  policy = data.aws_iam_policy_document.force_mfa.json
}

# Developers: look at the project's instances, start/stop only project-tagged ones, work with the audit bucket
data "aws_iam_policy_document" "developer" {
  statement {
    sid       = "DescribeOnly"
    actions   = ["ec2:Describe*", "cloudwatch:Get*", "cloudwatch:List*", "logs:Get*", "logs:Describe*"]
    resources = ["*"]
  }

  statement {
    sid       = "StartStopProjectInstances"
    actions   = ["ec2:StartInstances", "ec2:StopInstances"]
    resources = ["arn:aws:ec2:*:*:instance/*"]

    condition {
      test     = "StringEquals"
      variable = "aws:ResourceTag/project"
      values   = ["trustflow"]
    }
  }

  statement {
    sid       = "AuditBucket"
    actions   = ["s3:GetObject", "s3:PutObject", "s3:ListBucket"]
    resources = [aws_s3_bucket.audit.arn, "${aws_s3_bucket.audit.arn}/*"]
  }
}

resource "aws_iam_policy" "developer" {
  name   = "trustflow-developer"
  policy = data.aws_iam_policy_document.developer.json
}

resource "aws_iam_group_policy_attachment" "admins_mfa" {
  group      = aws_iam_group.admins.name
  policy_arn = aws_iam_policy.force_mfa.arn
}

resource "aws_iam_group_policy_attachment" "developers_mfa" {
  group      = aws_iam_group.developers.name
  policy_arn = aws_iam_policy.force_mfa.arn
}

resource "aws_iam_group_policy_attachment" "developers_policy" {
  group      = aws_iam_group.developers.name
  policy_arn = aws_iam_policy.developer.arn
}

# Admins do not hold standing power: they assume an operator role, and only with MFA
data "aws_iam_policy_document" "operator_trust" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"]
    }

    condition {
      test     = "Bool"
      variable = "aws:MultiFactorAuthPresent"
      values   = ["true"]
    }
  }
}

resource "aws_iam_role" "operator" {
  name                 = "trustflow-operator"
  assume_role_policy   = data.aws_iam_policy_document.operator_trust.json
  max_session_duration = 3600
}

resource "aws_iam_role_policy_attachment" "operator_power" {
  role       = aws_iam_role.operator.name
  policy_arn = "arn:aws:iam::aws:policy/PowerUserAccess"
}

data "aws_iam_policy_document" "admins_assume_operator" {
  statement {
    actions   = ["sts:AssumeRole"]
    resources = [aws_iam_role.operator.arn]
  }
}

resource "aws_iam_group_policy" "admins_assume_operator" {
  name   = "assume-operator"
  group  = aws_iam_group.admins.name
  policy = data.aws_iam_policy_document.admins_assume_operator.json
}
