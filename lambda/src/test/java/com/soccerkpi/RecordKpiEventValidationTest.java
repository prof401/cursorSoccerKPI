package com.soccerkpi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RecordKpiEvent request validation (null/empty body, missing gameId).
 * These tests do not hit DynamoDB; they only assert validation responses.
 */
class RecordKpiEventValidationTest {

    private static final Context MOCK_CONTEXT = new MockContext();

    @Test
    void recordKpiEvent_null_body_returns_400() {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        event.setPathParameters(Collections.singletonMap("gameId", "test-game-id"));
        event.setBody(null);

        APIGatewayV2HTTPResponse response = Handlers.recordKpiEvent(event, MOCK_CONTEXT);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("Request body is required"));
    }

    @Test
    void recordKpiEvent_empty_body_returns_400() {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        event.setPathParameters(Collections.singletonMap("gameId", "test-game-id"));
        event.setBody("");

        APIGatewayV2HTTPResponse response = Handlers.recordKpiEvent(event, MOCK_CONTEXT);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("Request body is required"));
    }

    @Test
    void recordKpiEvent_missing_gameId_returns_400() {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        event.setPathParameters(null);
        event.setBody("{\"kpiId\":\"goals\",\"delta\":1}");

        APIGatewayV2HTTPResponse response = Handlers.recordKpiEvent(event, MOCK_CONTEXT);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("gameId"));
    }
}
