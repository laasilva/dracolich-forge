package dm.dracolich.forge.exception;

import dm.dracolich.forge.error.ErrorCode;
import dm.dracolich.forge.error.ErrorCodes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationExceptionTest {

    @Test
    void holdsErrorsAndMessage() {
        List<ErrorCode> errors = List.of(ErrorCodes.DMD003, ErrorCodes.DMD006);
        ValidationException ex = new ValidationException(errors, "Validation failed");

        assertEquals("Validation failed", ex.getMessage());
        assertEquals(2, ex.getErrors().size());
        assertEquals(ErrorCodes.DMD003, ex.getErrors().getFirst());
    }

    @Test
    void isIllegalArgumentException() {
        ValidationException ex = new ValidationException(List.of(), "msg");

        assertInstanceOf(IllegalArgumentException.class, ex);
    }
}
