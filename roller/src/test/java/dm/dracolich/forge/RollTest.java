package dm.dracolich.forge;

import dm.dracolich.forge.to.Value;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RollTest {

    private static byte[] hmacSha256(String key, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    private static int firstFourBytesAsInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes, 0, 4).getInt();
    }

    private static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    @Test
    void seedChainAdvance_matchesKnownSha256() {
        String serverSeed = "secret";
        String expected = "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b";
        assertEquals(expected, Roll.seedChainAdvance(serverSeed));
    }

    @Test
    void validateHmac_roundTripTrue_andFalseOnMismatch() {
        String key = "secret";
        String msg = "client:1:value";
        String hex = Roll.hmacHex(key, msg);
        assertTrue(Roll.validateHmac(key, msg, hex));
        assertFalse(Roll.validateHmac(key, msg, hex + "00"));
    }

    @Test
    void d20_example_deterministic_itemIndex_and_range() throws Exception {
        Value d20 = Value.builder().id("D20").weight(1).count(20).build();
        String serverSeed = "server";
        String clientSeed = "client";
        long nonce = 42L;

        String itemMsg = Roll.buildMessage(clientSeed, nonce, "item");
        int itemValue = firstFourBytesAsInt(hmacSha256(serverSeed, itemMsg));
        int expectedIndex = Math.floorMod(itemValue, 20);

        Roll.FairRoll fair = Roll.fairRoll(serverSeed, clientSeed, nonce, List.of(d20), true);
        Map<String, Object> result = fair.result();

        assertEquals("D20", result.get("value"));
        assertEquals(expectedIndex, result.get("item"));
        assertTrue(expectedIndex >= 0 && expectedIndex < 20);
        assertEquals(sha256Hex(serverSeed), fair.nextServerSeed());
    }

    @Test
    void weighted_value_selection_matches_expected_bucket() throws Exception {
        Value a = Value.builder().id("A").weight(1).count(5).build();
        Value b = Value.builder().id("B").weight(100).count(5).build();
        List<Value> values = List.of(a, b);

        String serverSeed = "seed-alpha";
        String clientSeed = "player-x";
        long nonce = 7L;

        String valueMsg = Roll.buildMessage(clientSeed, nonce, "value");
        int valueValue = firstFourBytesAsInt(hmacSha256(serverSeed, valueMsg));
        int totalWeight = a.getWeight() + b.getWeight();
        int valueRoll = Math.floorMod(valueValue, totalWeight);
        String expectedvalue = valueRoll < a.getWeight() ? "A" : "B";

        Roll.FairRoll fair = Roll.fairRoll(serverSeed, clientSeed, nonce, values, false);
        assertEquals(expectedvalue, fair.result().get("value"));
        assertEquals(serverSeed, fair.nextServerSeed());
    }

    @Test
    void itemIndex_null_when_count_zero_or_null() {
        Value r0 = Value.builder().id("Empty").weight(1).count(0).build();
        Value rN = Value.builder().id("EmptyNull").weight(1).count(null).build();

        Roll.FairRoll fair0 = Roll.fairRoll("k", "c", 1, List.of(r0), true);
        assertNull(fair0.result().get("item"));

        Roll.FairRoll fairN = Roll.fairRoll("k", "c", 2, List.of(rN), true);
        assertNull(fairN.result().get("item"));
    }

    @Test
    void input_validation_errors() {
        assertThrows(IllegalArgumentException.class, () -> Roll.fairRoll("s", "c", 0, List.of(), true));
        Value zero = Value.builder().id("Z").weight(0).count(1).build();
        assertThrows(IllegalArgumentException.class, () -> Roll.fairRoll("s", "c", 0, List.of(zero), true));
    }

    @Test
    void helpers_drawIndex_and_drawWeighted_work() {
        String serverSeed = "srv";
        String clientSeed = "cli";
        long nonce = 99L;

        int idx = Roll.drawIndex(serverSeed, clientSeed, nonce, "dice", 20);
        assertTrue(idx >= 0 && idx < 20);

        Value a = Value.builder().id("A").weight(1).count(1).build();
        Value b = Value.builder().id("B").weight(5).count(1).build();
        Value chosen = Roll.drawWeighted(serverSeed, clientSeed, nonce, "value", List.of(a, b), f -> f.getWeight());
        assertTrue(chosen == a || chosen == b);
    }
}
