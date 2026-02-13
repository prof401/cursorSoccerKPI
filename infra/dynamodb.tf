resource "aws_dynamodb_table" "games" {
  name         = "${var.project_name}-games"
  billing_mode = "PAY_PER_REQUEST"

  hash_key = "gameId"

  attribute {
    name = "gameId"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = {
    Project = var.project_name
    Table   = "games"
  }
}

resource "aws_dynamodb_table" "kpi_definitions" {
  name         = "${var.project_name}-kpi-definitions"
  billing_mode = "PAY_PER_REQUEST"

  hash_key  = "gameId"
  range_key = "kpiId"

  attribute {
    name = "gameId"
    type = "S"
  }

  attribute {
    name = "kpiId"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = {
    Project = var.project_name
    Table   = "kpi_definitions"
  }
}

resource "aws_dynamodb_table" "kpi_events" {
  name         = "${var.project_name}-kpi-events"
  billing_mode = "PAY_PER_REQUEST"

  hash_key  = "gameId"
  range_key = "eventTimestamp"

  attribute {
    name = "gameId"
    type = "S"
  }

  attribute {
    name = "eventTimestamp"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  tags = {
    Project = var.project_name
    Table   = "kpi_events"
  }
}
