package dm.dracolich.forge.exception;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.error.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResponseExceptionTest {

    @Test
    void constructor_withMessage() {
        List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
        ResponseException ex = new ResponseException("Something failed", errors, HttpStatus.BAD_REQUEST);

        assertEquals("Something failed", ex.getMessage());
        assertEquals(errors, ex.getErrors());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void constructor_withoutMessage() {
        List<ApiError> errors = List.of(new ApiError(ErrorCodes.DMD001));
        ResponseException ex = new ResponseException(errors, HttpStatus.INTERNAL_SERVER_ERROR);

        assertNull(ex.getMessage());
        assertEquals(errors, ex.getErrors());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
    }

    @Test
    void isRuntimeException() {
        ResponseException ex = new ResponseException(List.of(), HttpStatus.OK);

        assertInstanceOf(RuntimeException.class, ex);
    }
}
