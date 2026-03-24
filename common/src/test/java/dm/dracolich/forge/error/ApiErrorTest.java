package dm.dracolich.forge.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorTest {

    @Test
    void constructor_withErrorCode() {
        ApiError error = new ApiError(ErrorCodes.DMD001);

        assertEquals(ErrorCodes.DMD001, error.getError());
        assertNull(error.getSeverity());
        assertNull(error.getField());
    }

    @Test
    void constructor_withAllFields() {
        ApiError error = new ApiError(ErrorCodes.DMD003, ErrorSeverity.WARNING, "username");

        assertEquals(ErrorCodes.DMD003, error.getError());
        assertEquals(ErrorSeverity.WARNING, error.getSeverity());
        assertEquals("username", error.getField());
    }
}
