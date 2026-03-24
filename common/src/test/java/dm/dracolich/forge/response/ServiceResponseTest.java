package dm.dracolich.forge.response;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceResponseTest {

    @Test
    void constructor_withPayload_isSuccess() {
        ServiceResponse<String> response = new ServiceResponse<>("data");

        assertTrue(response.success());
        assertFalse(response.failure());
        assertEquals("data", response.getPayload());
        assertEquals("data", response.payload());
        assertNull(response.getErrors());
    }

    @Test
    void constructor_withErrors_isFailure() {
        List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
        ServiceResponse<Object> response = new ServiceResponse<>(errors);

        assertFalse(response.success());
        assertTrue(response.failure());
        assertNull(response.getPayload());
        assertEquals(1, response.getErrors().size());
    }

    @Test
    void fullConstructor() {
        ServiceResponse<String> response = new ServiceResponse<>(true, "ok", null);

        assertTrue(response.success());
        assertEquals("ok", response.getPayload());
    }

    @Test
    void staticOf_payload() {
        ServiceResponse<Integer> response = ServiceResponse.of(42);

        assertTrue(response.success());
        assertEquals(42, response.payload());
    }

    @Test
    void staticOf_errors() {
        List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
        ServiceResponse<Object> response = ServiceResponse.of(errors);

        assertFalse(response.success());
        assertEquals(errors, response.getErrors());
    }

    @Test
    void setPayload_switchesToSuccess() {
        ServiceResponse<String> response = new ServiceResponse<>(List.of(new ApiError(ErrorCodes.DMD001)));

        response.setPayload("recovered");

        assertTrue(response.success());
        assertEquals("recovered", response.getPayload());
    }

    @Test
    void setError_switchesToFailure() {
        ServiceResponse<String> response = new ServiceResponse<>("ok");

        response.setError(List.of(new ApiError(ErrorCodes.DMD001)));

        assertFalse(response.success());
    }

    @Test
    void addError() {
        List<ApiError> errors = new ArrayList<>();
        errors.add(new ApiError(ErrorCodes.DMD001));
        ServiceResponse<Object> response = new ServiceResponse<>(errors);

        response.addError(new ApiError(ErrorCodes.DMD002));

        assertEquals(2, response.getErrors().size());
    }

    @Test
    void addErrorFromErrorCode() {
        List<ApiError> errors = new ArrayList<>();
        ServiceResponse<Object> response = new ServiceResponse<>(true, null, errors);

        response.addErrorFromErrorCode(ErrorCodes.DMD003);

        assertEquals(1, response.getErrors().size());
    }
}
