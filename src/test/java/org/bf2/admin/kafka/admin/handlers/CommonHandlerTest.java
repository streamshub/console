package org.bf2.admin.kafka.admin.handlers;

import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.admin.model.Types;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommonHandlerTest {

    /**
     * InvalidRequestException is expected to be thrown by the custom ACL
     * authorizer in the kas-broker-plugins library. Check this case via unit
     * test (rather than system test) since the Kafka image used in the
     * systemtests module does not (yet) include the kas-broker-plugins.
     */
    @Test
    void testProcessFailureWithInvalidRequestException() {
        InvalidRequestException cause = new InvalidRequestException("The request has failed");
        Response response = CommonHandler.processFailure(cause).build();
        Types.Error errorEntity = (Types.Error) response.getEntity();
        assertEquals(ErrorType.INVALID_CONFIGURATION.getHttpStatus().getStatusCode(), errorEntity.getCode());
        assertEquals(ErrorType.INVALID_CONFIGURATION.getReason(), errorEntity.getReason());
        assertEquals(cause.getMessage(), errorEntity.getDetail());
    }

    @Test
    void testProcessFailureWithPolicyViolationException() {
        PolicyViolationException cause = new PolicyViolationException("The request has failed");
        Response response = CommonHandler.processFailure(cause).build();
        Types.Error errorEntity = (Types.Error) response.getEntity();
        assertEquals(ErrorType.POLICY_VIOLATION.getHttpStatus().getStatusCode(), errorEntity.getCode());
        assertEquals(ErrorType.POLICY_VIOLATION.getReason(), errorEntity.getReason());
        assertEquals(cause.getMessage(), errorEntity.getDetail());
    }
}
