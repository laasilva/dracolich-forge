package dm.dracolich.forge;

import dm.dracolich.forge.to.Value;

import java.util.List;
import java.util.stream.IntStream;

public class Dice {
    public static List<Value> d4() {
        return IntStream.rangeClosed(1, 4)
                .mapToObj(i -> new Value(String.valueOf(i), 1, 4))
            .toList();
    }

    public static List<Value> d6() {
        return IntStream.rangeClosed(1, 6)
                .mapToObj(i -> new Value(String.valueOf(i), 1, 6))
            .toList();
    }

    public static List<Value> d8() {
        return IntStream.rangeClosed(1, 8)
                .mapToObj(i -> new Value(String.valueOf(i), 1, 8))
            .toList();
    }

    public static List<Value> d10() {
        return IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Value(String.valueOf(i), 1, 10))
            .toList();
    }

    public static List<Value> d12() {
        return IntStream.rangeClosed(1, 12)
                .mapToObj(i -> new Value(String.valueOf(i), 1, 12))
            .toList();
    }

    public static List<Value> d20() {
        return IntStream.rangeClosed(1, 20)
                .mapToObj(i -> new Value(String.valueOf(i), 1, 20))
            .toList();
    }

    public static List<Value> d100() {
        return IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new Value(String.format("%02d", (i % 10) * 10), 1, 10))
            .toList();
    }
}
