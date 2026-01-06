package dm.dracolich.forge;

import dm.dracolich.forge.to.DiceEnum;
import dm.dracolich.forge.to.Value;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiceTest {

    @Test
    void d4_returns_4_values_with_correct_properties() {
        List<Value> values = Dice.d4();

        assertEquals(4, values.size());
        for (int i = 0; i < 4; i++) {
            Value v = values.get(i);
            assertEquals(String.valueOf(i + 1), v.getId());
            assertEquals(1, v.getWeight());
            assertEquals(4, v.getCount());
        }
    }

    @Test
    void d6_returns_6_values_with_correct_properties() {
        List<Value> values = Dice.d6();

        assertEquals(6, values.size());
        for (int i = 0; i < 6; i++) {
            Value v = values.get(i);
            assertEquals(String.valueOf(i + 1), v.getId());
            assertEquals(1, v.getWeight());
            assertEquals(6, v.getCount());
        }
    }

    @Test
    void d8_returns_8_values_with_correct_properties() {
        List<Value> values = Dice.d8();

        assertEquals(8, values.size());
        for (int i = 0; i < 8; i++) {
            Value v = values.get(i);
            assertEquals(String.valueOf(i + 1), v.getId());
            assertEquals(1, v.getWeight());
            assertEquals(8, v.getCount());
        }
    }

    @Test
    void d10_returns_10_values_with_correct_properties() {
        List<Value> values = Dice.d10();

        assertEquals(10, values.size());
        for (int i = 0; i < 10; i++) {
            Value v = values.get(i);
            assertEquals(String.valueOf(i + 1), v.getId());
            assertEquals(1, v.getWeight());
            assertEquals(10, v.getCount());
        }
    }

    @Test
    void d12_returns_12_values_with_correct_properties() {
        List<Value> values = Dice.d12();

        assertEquals(12, values.size());
        for (int i = 0; i < 12; i++) {
            Value v = values.get(i);
            assertEquals(String.valueOf(i + 1), v.getId());
            assertEquals(1, v.getWeight());
            assertEquals(12, v.getCount());
        }
    }

    @Test
    void d20_returns_20_values_with_correct_properties() {
        List<Value> values = Dice.d20();

        assertEquals(20, values.size());
        for (int i = 0; i < 20; i++) {
            Value v = values.get(i);
            assertEquals(String.valueOf(i + 1), v.getId());
            assertEquals(1, v.getWeight());
            assertEquals(20, v.getCount());
        }
    }

    @Test
    void d100_returns_10_values_with_correct_properties() {
        List<Value> values = Dice.d100();

        assertEquals(10, values.size());
        // Values are: 10, 20, 30, 40, 50, 60, 70, 80, 90, 00 (where 00 = 100)
        String[] expectedIds = {"10", "20", "30", "40", "50", "60", "70", "80", "90", "00"};
        for (int i = 0; i < 10; i++) {
            Value v = values.get(i);
            assertEquals(expectedIds[i], v.getId());
            assertEquals(1, v.getWeight());
            assertEquals(10, v.getCount());
        }
    }

    @Test
    void diceEnum_of_throws_and_logs_on_invalid_dice() {
        DiceEnum d20 = DiceEnum.D20;
        assertThrows(IllegalArgumentException.class, () -> d20.of("invalid"));
    }
}
