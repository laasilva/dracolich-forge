package dm.dracolich.forge.response;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCodes;
import dm.dracolich.forge.error.ErrorSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DmdResponseTest {

    @Test
    void constructor_withPayload_isSuccess() {
        DmdResponse<String> response = new DmdResponse<>("hello");

        assertTrue(response.getSuccess());
        assertTrue(response.success());
        assertFalse(response.failure());
        assertEquals("hello", response.getPayload());
        assertEquals("hello", response.payload());
        assertEquals(HttpStatus.OK, response.getHttpStatus());
        assertEquals("Request processed successfully", response.getMessage());
        assertNull(response.getErrors());
    }

    @Test
    void constructor_withPayloadAndMessage_isSuccess() {
        DmdResponse<Integer> response = new DmdResponse<>(42, "Custom message");

        assertTrue(response.getSuccess());
        assertEquals(42, response.getPayload());
        assertEquals("Custom message", response.getMessage());
        assertEquals(HttpStatus.OK, response.getHttpStatus());
    }

    @Test
    void constructor_withPayloadStatusMessage_isFailure() {
        DmdResponse<String> response = new DmdResponse<>(null, HttpStatus.NOT_FOUND, "Not found");

        assertFalse(response.getSuccess());
        assertNull(response.getPayload());
        assertEquals(HttpStatus.NOT_FOUND, response.getHttpStatus());
        assertEquals("Not found", response.getMessage());
    }

    @Test
    void constructor_withErrors_isFailure() {
        List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
        DmdResponse<Object> response = new DmdResponse<>(errors);

        assertFalse(response.getSuccess());
        assertNull(response.getPayload());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getHttpStatus());
        assertEquals("Request ended with errors", response.getMessage());
        assertEquals(1, response.getErrors().size());
    }

    @Test
    void constructor_withErrorsAndStatus() {
        List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
        DmdResponse<Object> response = new DmdResponse<>(errors, HttpStatus.BAD_REQUEST);

        assertFalse(response.getSuccess());
        assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
        assertEquals("Request ended with errors", response.getMessage());
    }

    @Test
    void constructor_withErrorsStatusAndMessage() {
        List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
        DmdResponse<Object> response = new DmdResponse<>(errors, HttpStatus.FORBIDDEN, "Denied");

        assertFalse(response.getSuccess());
        assertEquals(HttpStatus.FORBIDDEN, response.getHttpStatus());
        assertEquals("Denied", response.getMessage());
    }

    @Test
    void fullConstructor() {
        DmdResponse<String> response = new DmdResponse<>(true, "data", null, HttpStatus.OK, "ok");

        assertTrue(response.getSuccess());
        assertEquals("data", response.getPayload());
        assertEquals(HttpStatus.OK, response.getHttpStatus());
        assertEquals("ok", response.getMessage());
    }

    @Test
    void staticOf_withPayload() {
        DmdResponse<String> response = DmdResponse.of("payload", HttpStatus.CREATED, "Created");

        assertTrue(response.getSuccess());
        assertEquals("payload", response.getPayload());
        assertEquals(HttpStatus.CREATED, response.getHttpStatus());
        assertEquals("Created", response.getMessage());
    }

    @Test
    void staticOf_withErrors() {
        List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
        DmdResponse<Object> response = DmdResponse.of(errors, HttpStatus.BAD_REQUEST, "Bad");

        assertFalse(response.getSuccess());
        assertNull(response.getPayload());
        assertEquals(errors, response.getErrors());
        assertEquals(HttpStatus.BAD_REQUEST, response.getHttpStatus());
    }

    @Test
    void setPayload_switchesToSuccess() {
        DmdResponse<String> response = new DmdResponse<>(null, HttpStatus.BAD_REQUEST, "error");
        assertFalse(response.getSuccess());

        response.setPayload("recovered");

        assertTrue(response.getSuccess());
        assertEquals("recovered", response.getPayload());
    }

    @Test
    void setError_switchesToFailure() {
        DmdResponse<String> response = new DmdResponse<>("ok");
        assertTrue(response.getSuccess());

        response.setError(List.of(new ApiError(ErrorCodes.DMD001)));

        assertFalse(response.getSuccess());
        assertEquals(1, response.getErrors().size());
    }

    @Test
    void addError() {
        List<ApiError> errors = new ArrayList<>();
        errors.add(new ApiError(ErrorCodes.DMD001));
        DmdResponse<Object> response = new DmdResponse<>(errors);

        response.addError(new ApiError(ErrorCodes.DMD002));

        assertEquals(2, response.getErrors().size());
    }

    @Test
    void addErrorFromErrorCode() {
        List<ApiError> errors = new ArrayList<>();
        DmdResponse<Object> response = new DmdResponse<>(false, null, errors, HttpStatus.BAD_REQUEST, "err");

        response.addErrorFromErrorCode(ErrorCodes.DMD003);

        assertEquals(1, response.getErrors().size());
        assertEquals("DMD003", response.getErrors().getFirst().getError().getCode());
    }

    @Test
    void addError_lazyInitializesWhenNull() {
        DmdResponse<String> response = new DmdResponse<>("data");
        assertNull(response.getErrors());

        response.addError(new ApiError(ErrorCodes.DMD001));

        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
    }

    @Test
    void addErrorFromErrorCode_lazyInitializesWhenNull() {
        DmdResponse<String> response = new DmdResponse<>("data", "message");
        assertNull(response.getErrors());

        response.addErrorFromErrorCode(ErrorCodes.DMD002);

        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
        assertEquals("DMD002", response.getErrors().getFirst().getError().getCode());
    }

    @Test
    void toResponseEntity() {
        DmdResponse<String> response = new DmdResponse<>("data");

        ResponseEntity<String> entity = response.toResponseEntity();

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals("data", entity.getBody());
    }
}
