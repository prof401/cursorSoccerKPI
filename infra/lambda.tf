locals {
  lambda_runtime = "java21"
}

resource "aws_lambda_function" "create_game" {
  function_name = "${var.project_name}-createGame"
  role          = aws_iam_role.lambda_role.arn
  runtime       = local.lambda_runtime
  handler       = "com.soccerkpi.Handlers::createGame"

  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)

  environment {
    variables = {
      GAMES_TABLE           = aws_dynamodb_table.games.name
      KPI_DEFINITIONS_TABLE = aws_dynamodb_table.kpi_definitions.name
      KPI_EVENTS_TABLE      = aws_dynamodb_table.kpi_events.name
    }
  }

  memory_size = 512
  timeout     = 15
}

resource "aws_lambda_function" "get_kpi_definitions" {
  function_name = "${var.project_name}-getKpiDefinitions"
  role          = aws_iam_role.lambda_role.arn
  runtime       = local.lambda_runtime
  handler       = "com.soccerkpi.Handlers::getKpiDefinitions"

  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)

  environment {
    variables = {
      GAMES_TABLE           = aws_dynamodb_table.games.name
      KPI_DEFINITIONS_TABLE = aws_dynamodb_table.kpi_definitions.name
      KPI_EVENTS_TABLE      = aws_dynamodb_table.kpi_events.name
    }
  }

  memory_size = 512
  timeout     = 15
}

resource "aws_lambda_function" "record_kpi_event" {
  function_name = "${var.project_name}-recordKpiEvent"
  role          = aws_iam_role.lambda_role.arn
  runtime       = local.lambda_runtime
  handler       = "com.soccerkpi.Handlers::recordKpiEvent"

  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)

  environment {
    variables = {
      GAMES_TABLE           = aws_dynamodb_table.games.name
      KPI_DEFINITIONS_TABLE = aws_dynamodb_table.kpi_definitions.name
      KPI_EVENTS_TABLE      = aws_dynamodb_table.kpi_events.name
    }
  }

  memory_size = 512
  timeout     = 15
}

resource "aws_lambda_function" "get_game_summary" {
  function_name = "${var.project_name}-getGameSummary"
  role          = aws_iam_role.lambda_role.arn
  runtime       = local.lambda_runtime
  handler       = "com.soccerkpi.Handlers::getGameSummary"

  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)

  environment {
    variables = {
      GAMES_TABLE           = aws_dynamodb_table.games.name
      KPI_DEFINITIONS_TABLE = aws_dynamodb_table.kpi_definitions.name
      KPI_EVENTS_TABLE      = aws_dynamodb_table.kpi_events.name
    }
  }

  memory_size = 512
  timeout     = 20
}

resource "aws_lambda_function" "health" {
  function_name = "${var.project_name}-health"
  role          = aws_iam_role.lambda_role.arn
  runtime       = local.lambda_runtime
  handler       = "com.soccerkpi.Handlers::health"

  filename         = var.lambda_jar_path
  source_code_hash = filebase64sha256(var.lambda_jar_path)

  environment {
    variables = {
      GAMES_TABLE           = aws_dynamodb_table.games.name
      KPI_DEFINITIONS_TABLE = aws_dynamodb_table.kpi_definitions.name
      KPI_EVENTS_TABLE      = aws_dynamodb_table.kpi_events.name
    }
  }

  memory_size = 256
  timeout     = 5
}
