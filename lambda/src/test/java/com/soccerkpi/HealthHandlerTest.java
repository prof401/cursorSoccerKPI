package com.soccerkpi;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthHandlerTest {

    @Test
    void health_returns_200_and_ok_status() {
        APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        Context context = new MockContext();

        APIGatewayV2HTTPResponse response = Handlers.health(event, context);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody() != null && response.getBody().contains("\"status\":\"ok\""));
    }
}
