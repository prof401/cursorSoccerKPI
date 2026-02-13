package com.soccerkpi;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * Minimal Context implementation for unit tests. Only getAwsRequestId() and getRequestId() are used by handlers.
 */
public class MockContext implements Context {
    private final String requestId;

    public MockContext() {
        this("test-request-id");
    }

    public MockContext(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String getAwsRequestId() {
        return requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public String getLogGroupName() {
        return null;
    }

    @Override
    public String getLogStreamName() {
        return null;
    }

    @Override
    public String getFunctionName() {
        return "test-function";
    }

    @Override
    public String getFunctionVersion() {
        return "1";
    }

    @Override
    public String getInvokedFunctionArn() {
        return null;
    }

    @Override
    public int getMemoryLimitInMB() {
        return 512;
    }

    @Override
    public int getRemainingTimeInMillis() {
        return 15000;
    }

    @Override
    public ClientContext getClientContext() {
        return null;
    }

    @Override
    public CognitoIdentity getIdentity() {
        return null;
    }

    @Override
    public LambdaLogger getLogger() {
        return null;
    }
}
