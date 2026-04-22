package dm.dracolich.forge.controller;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCode;
import dm.dracolich.forge.error.ErrorCodes;
import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.forge.exception.ValidationException;
import dm.dracolich.forge.response.DmdResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControllerAdviceTest {

    private final ControllerAdvice advice = new ControllerAdvice();

    @Nested
    class HandleResponseException {

        @Test
        void returnsStatusAndMessage() {
            List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
            ResponseException ex = new ResponseException("Bad input", errors, HttpStatus.BAD_REQUEST);

            ResponseEntity<DmdResponse<?>> response = advice.handleResponseException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().getSuccess());
            assertEquals("Bad input", response.getBody().getMessage());
            assertEquals(1, response.getBody().getErrors().size());
            assertNull(response.getBody().getPayload());
        }

        @Test
        void handlesNullMessage() {
            List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD009));
            ResponseException ex = new ResponseException(errors, HttpStatus.NOT_FOUND);

            ResponseEntity<DmdResponse<?>> response = advice.handleResponseException(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNull(response.getBody().getMessage());
        }

        @Test
        void preservesHttpStatus() {
            ResponseException ex = new ResponseException("Denied", List.of(), HttpStatus.FORBIDDEN);

            ResponseEntity<DmdResponse<?>> response = advice.handleResponseException(ex);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals(HttpStatus.FORBIDDEN, response.getBody().getHttpStatus());
        }
    }

    @Nested
    class HandleValidationException {

        @Test
        void returnsBadRequestWithMappedErrors() {
            List<ErrorCode> errorCodes = List.of(ErrorCodes.DMD003, ErrorCodes.DMD006);
            ValidationException ex = new ValidationException(errorCodes, "Validation failed");

            ResponseEntity<DmdResponse<?>> response = advice.handleValidationException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().getSuccess());
            assertEquals("Validation failed", response.getBody().getMessage());
            assertEquals(2, response.getBody().getErrors().size());
            assertEquals("DMD003", response.getBody().getErrors().get(0).getError().getCode());
            assertEquals("DMD006", response.getBody().getErrors().get(1).getError().getCode());
        }

        @Test
        void handlesEmptyErrorList() {
            ValidationException ex = new ValidationException(List.of(), "No errors");

            ResponseEntity<DmdResponse<?>> response = advice.handleValidationException(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().getErrors().isEmpty());
        }
    }

    @Nested
    class HandleGenericException {

        @Test
        void returns500WithDmd001Error() {
            Exception ex = new RuntimeException("Something unexpected");

            ResponseEntity<DmdResponse<?>> response = advice.handleGenericException(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().getSuccess());
            assertEquals("An unexpected error occurred", response.getBody().getMessage());
            assertEquals(1, response.getBody().getErrors().size());
            assertEquals("DMD001", response.getBody().getErrors().getFirst().getError().getCode());
        }
    }
}
