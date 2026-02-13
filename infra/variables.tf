variable "aws_region" {
  type        = string
  description = "AWS region to deploy to"
  default     = "us-east-1"
}

variable "project_name" {
  type        = string
  description = "Project name prefix for resources"
  default     = "soccer-kpi-mvp"
}

variable "cors_origins" {
  type        = list(string)
  description = "Allowed CORS origins (e.g. [\"https://yourdomain.com\"] for production; [\"*\"] for dev)"
  default     = ["*"]
}

variable "lambda_jar_path" {
  type        = string
  description = "Path to the built Lambda uber-jar"
  default     = "../lambda/target/lambda.jar"
}
