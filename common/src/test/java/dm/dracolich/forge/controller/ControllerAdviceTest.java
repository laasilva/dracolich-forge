package dm.dracolich.forge.controller;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCodes;
import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.forge.response.DmdResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControllerAdviceTest {

    private final ControllerAdvice advice = new ControllerAdvice();

    @Test
    void handlesResponseException_withMessage() {
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
    void handlesResponseException_withoutMessage() {
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
