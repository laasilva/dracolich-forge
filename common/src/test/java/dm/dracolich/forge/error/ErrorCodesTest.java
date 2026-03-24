package dm.dracolich.forge.error;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodesTest {

    @Test
    void allCodesImplementErrorCode() {
        for (ErrorCodes code : ErrorCodes.values()) {
            assertNotNull(code.getCode());
            assertNotNull(code.getMessage());
            assertTrue(code.getCode().startsWith("DMD"));
        }
    }

    @Test
    void codesAreUnique() {
        var codes = java.util.Arrays.stream(ErrorCodes.values())
                .map(ErrorCodes::getCode)
                .toList();
        assertEquals(codes.size(), codes.stream().distinct().count());
    }

    @Test
    void format_replacesPlaceholders() {
        String formatted = ErrorCodes.DMD002.format("Entity", "123");

        assertEquals("Error while creating [Entity]={123}", formatted);
    }

    @Test
    void format_multipleArgs() {
        String formatted = ErrorCodes.DMD009.format("Race", "name", "Elf");

        assertEquals("{Race} [name]={Elf} not found.", formatted);
    }
}
