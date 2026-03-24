package dm.dracolich.forge.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DmdErrorTest {

    @Test
    void build() {
        DmdError error = DmdError.build("ERR001", "Something failed");

        assertEquals("ERR001", error.getCode());
        assertEquals("Something failed", error.getMessage());
    }

    @Test
    void noArgConstructor() {
        DmdError error = new DmdError();

        assertNull(error.getCode());
        assertNull(error.getMessage());
    }

    @Test
    void allArgConstructor() {
        DmdError error = new DmdError("C1", "M1");

        assertEquals("C1", error.getCode());
        assertEquals("M1", error.getMessage());
    }
}
