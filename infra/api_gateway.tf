resource "aws_apigatewayv2_api" "http_api" {
  name          = "${var.project_name}-http-api"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = var.cors_origins
    allow_methods = ["GET", "POST", "OPTIONS"]
    allow_headers = ["*"]
    max_age       = 300
  }
}

resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.http_api.id
  name        = "$default"
  auto_deploy = true
}

resource "aws_lambda_permission" "api_invoke_create_game" {
  statement_id  = "AllowAPIGatewayInvokeCreateGame"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.create_game.arn
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "api_invoke_get_kpi_definitions" {
  statement_id  = "AllowAPIGatewayInvokeGetKpiDefinitions"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.get_kpi_definitions.arn
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "api_invoke_record_kpi_event" {
  statement_id  = "AllowAPIGatewayInvokeRecordKpiEvent"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.record_kpi_event.arn
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "api_invoke_get_game_summary" {
  statement_id  = "AllowAPIGatewayInvokeGetGameSummary"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.get_game_summary.arn
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

resource "aws_lambda_permission" "api_invoke_health" {
  statement_id  = "AllowAPIGatewayInvokeHealth"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.health.arn
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.http_api.execution_arn}/*/*"
}

resource "aws_apigatewayv2_integration" "create_game" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type        = "AWS_PROXY"
  integration_uri         = aws_lambda_function.create_game.arn
  integration_method      = "POST"
  payload_format_version  = "2.0"
}

resource "aws_apigatewayv2_integration" "get_kpi_definitions" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type        = "AWS_PROXY"
  integration_uri         = aws_lambda_function.get_kpi_definitions.arn
  integration_method      = "GET"
  payload_format_version  = "2.0"
}

resource "aws_apigatewayv2_integration" "record_kpi_event" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type        = "AWS_PROXY"
  integration_uri         = aws_lambda_function.record_kpi_event.arn
  integration_method      = "POST"
  payload_format_version  = "2.0"
}

resource "aws_apigatewayv2_integration" "get_game_summary" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type        = "AWS_PROXY"
  integration_uri         = aws_lambda_function.get_game_summary.arn
  integration_method      = "GET"
  payload_format_version  = "2.0"
}

resource "aws_apigatewayv2_integration" "health" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type        = "AWS_PROXY"
  integration_uri         = aws_lambda_function.health.arn
  integration_method      = "GET"
  payload_format_version  = "2.0"
}

resource "aws_apigatewayv2_route" "create_game" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "POST /games"
  target    = "integrations/${aws_apigatewayv2_integration.create_game.id}"
}

resource "aws_apigatewayv2_route" "get_kpi_definitions" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "GET /games/{gameId}/kpis"
  target    = "integrations/${aws_apigatewayv2_integration.get_kpi_definitions.id}"
}

resource "aws_apigatewayv2_route" "record_kpi_event" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "POST /games/{gameId}/events"
  target    = "integrations/${aws_apigatewayv2_integration.record_kpi_event.id}"
}

resource "aws_apigatewayv2_route" "get_game_summary" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "GET /games/{gameId}/summary"
  target    = "integrations/${aws_apigatewayv2_integration.get_game_summary.id}"
}

resource "aws_apigatewayv2_route" "health" {
  api_id    = aws_apigatewayv2_api.http_api.id
  route_key = "GET /health"
  target    = "integrations/${aws_apigatewayv2_integration.health.id}"
}
