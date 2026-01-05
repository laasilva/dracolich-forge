package dm.dracolich.forge;

import dm.dracolich.forge.to.Debug;
import dm.dracolich.forge.to.Rollout;
import dm.dracolich.forge.to.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.ToIntFunction;

public class Roll {
    /**
     * Compute the HMAC-SHA256 of a message using a given key.
     * Returns the HMAC as a hexadecimal string.
     *
     * @param key the key to use for the HMAC computation
     * @param message the message to compute the HMAC of
     * @return the HMAC of the message as a hexadecimal string
     */
    public static String hmacHex(String key, String message) {
        return bytesToHex(hmacBytes(key, message));
    }

    /**
     * Compute the HMAC-SHA256 of a message using a given key.
     * Returns the HMAC as a byte array.
     *
     * @param key the key to use for the HMAC computation
     * @param message the message to compute the HMAC of
     * @return the HMAC of the message as a byte array
     * @throws RuntimeException if an exception occurs during the HMAC computation
     */
    public static byte[] hmacBytes(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");

            mac.init(secretKeySpec);

            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    /**
     * Computes the SHA-256 hash of a given string value.
     *
     * @param value the string value to hash
     * @return the SHA-256 hash of the given string value as a hexadecimal string
     * @throws RuntimeException if an exception occurs during the hash computation
     */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256", e);
        }
    }

    /**
     * Advance the server seed by computing its SHA-256 hash and
     * returning the result as a hexadecimal string.
     *
     * @param serverSeed the server seed to advance
     * @return the advanced server seed as a hexadecimal string
     */
    public static String seedChainAdvance(String serverSeed) {
        return sha256Hex(serverSeed);
    }

    /**
     * Pseudorandom function (PRF) utilities for generating deterministic random values.
     * <p>
     * This class provides methods to derive pseudorandom integers and hexadecimal strings
     * from a {@link RollContext} using HMAC-SHA256.
     */
    public static final class Prf {
        /**
         * Draws a pseudorandom integer from the given roll context and category.
         * <p>
         * Computes an HMAC-SHA256 of the message built from the context's client seed,
         * nonce, and the provided category, using the server seed as the key.
         * The first four bytes of the resulting HMAC are interpreted as a big-endian integer.
         *
         * @param ctx the roll context containing server seed, client seed, and nonce
         * @param category the category identifier for this draw operation
         * @return a pseudorandom integer derived from the HMAC
         */
        public static int drawInt(RollContext ctx, String category) {
            byte[] bytes = hmacBytes(ctx.serverSeed(), buildMessage(ctx.clientSeed(), ctx.nonce(), category));

            return firstFourBytesAsInt(bytes);
        }

        /**
         * Draws a pseudorandom hexadecimal string from the given roll context and category.
         * <p>
         * Computes an HMAC-SHA256 of the message built from the context's client seed,
         * nonce, and the provided category, using the server seed as the key.
         *
         * @param ctx the roll context containing server seed, client seed, and nonce
         * @param category the category identifier for this draw operation
         * @return the full HMAC-SHA256 result as a hexadecimal string
         */
        public static String drawHex(RollContext ctx, String category) {
            return hmacHex(ctx.serverSeed(), buildMessage(ctx.clientSeed(), ctx.nonce(), category));
        }
    }

    /**
     * Selection utilities for choosing items from collections using a draw value.
     * <p>
     * This class provides methods to select items either by index or by weighted probability,
     * using a precomputed draw value (typically from {@link Prf}).
     */
    public static final class Select {
        /**
         * Selects an item from a weighted list using a draw value.
         * <p>
         * The draw value is mapped to the total weight range using modular arithmetic,
         * then used to select an item based on cumulative weights. Items with higher
         * weights have proportionally higher chances of being selected.
         *
         * @param <T> the type of items in the list
         * @param items the list of items to select from; must be non-null and non-empty
         * @param weightFn a function that returns the weight for each item; negative weights are treated as zero
         * @param draw the draw value used for selection
         * @return the selected item from the list
         * @throws IllegalArgumentException if items is null, empty, or all weights sum to zero or less
         * @throws IllegalStateException if selection fails unexpectedly
         */
        public static <T> T weightedChoice(List<T> items, ToIntFunction<T> weightFn, int draw) {
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("items must be non-empty");
            }

            int total = 0;

            for (T t : items) {
                int w = Math.max(0, weightFn.applyAsInt(t));
                total += w;
            }

            if (total <= 0)
                throw new IllegalArgumentException("Sum of weights must be positive");

            int roll = Math.floorMod(draw, total);
            int c = 0;

            for (T t : items) {
                c += Math.max(0, weightFn.applyAsInt(t));
                if (roll < c) return t;
            }

            throw new IllegalStateException("Failed to select from weights");
        }

        /**
         * Selects an index from a range [0, size) using a draw value.
         * <p>
         * The draw value is mapped to the valid index range using modular arithmetic,
         * ensuring uniform distribution across all indices.
         *
         * @param size the upper bound (exclusive) of the index range
         * @param draw the draw value used for selection
         * @return an index in the range [0, size), or -1 if size is not positive
         */
        public static int indexChoice(int size, int draw) {
            if (size <= 0) return -1;
            return Math.floorMod(draw, size);
        }
    }


    /**
     * Draws a random index from a range [0, size) using provably fair randomness.
     * <p>
     * This method computes an HMAC-SHA256 using the server seed, client seed, nonce,
     * and category, then maps the result to an index within the specified size.
     *
     * @param serverSeed the server seed used for HMAC computation
     * @param clientSeed the client seed used for HMAC computation
     * @param nonce the nonce to ensure uniqueness of each draw
     * @param category the category identifier for the draw operation
     * @param size the upper bound (exclusive) of the index range; must be positive
     * @return a random index in the range [0, size), or -1 if size is not positive
     */
    public static int drawIndex(String serverSeed, String clientSeed, long nonce, String category, int size) {
        return Select.indexChoice(size, Prf.drawInt(new RollContext(serverSeed, clientSeed, nonce), category));
        
    }

    /**
     * Selects an item from a weighted list using provably fair randomness.
     * <p>
     * This method computes an HMAC-SHA256 using the server seed, client seed, nonce,
     * and category, then uses the result to perform a weighted random selection
     * from the provided list of items.
     *
     * @param <T> the type of items in the list
     * @param serverSeed the server seed used for HMAC computation
     * @param clientSeed the client seed used for HMAC computation
     * @param nonce the nonce to ensure uniqueness of each draw
     * @param category the category identifier for the draw operation
     * @param items the list of items to select from; must be non-empty
     * @param weightFn a function that returns the weight for each item; weights must be non-negative
     * @return the selected item from the list
     * @throws IllegalArgumentException if items is null, empty, or all weights sum to zero or less
     * @throws IllegalStateException if selection fails unexpectedly
     */
    public static <T> T drawWeighted(String serverSeed, String clientSeed, long nonce, String category,
                                     List<T> items, ToIntFunction<T> weightFn) {
        return Select.weightedChoice(items, weightFn,
                Prf.drawInt(new RollContext(serverSeed, clientSeed, nonce), category));
    }

    /**
     * Fairly select a value from a given list based on the server seed,
     * client seed, and nonce, and return the result as a FairRoll object.
     *
     * The server seed is used to generate the HMAC-SHA256 of the client seed,
     * nonce, and message "value". The first four bytes of this HMAC are
     * interpreted as an unsigned integer, and this value is used to select
     * a value from the given list. The selected value is then used to
     * generate the HMAC-SHA256 of the client seed, nonce, and message "item".
     * The first four bytes of this HMAC are interpreted as an unsigned integer,
     * and this value is used to select an item from the selected value.
     *
     * @param serverSeed the server seed to use for the HMAC computation
     * @param clientSeed the client seed to use for the HMAC computation
     * @param nonce the nonce to use for the HMAC computation
     * @param values the list of values to select from
     * @param advanceServerSeed whether to advance the server seed by computing its
     *            SHA-256 hash and returning the result as a hexadecimal string
     * @return the result as a FairRoll object
     * @throws IllegalArgumentException if the given list of values is null or empty
     * @throws IllegalStateException if the selected value has no items
     */
    public static FairRoll fairRoll(String serverSeed, String clientSeed, long nonce,
            List<Value> values, boolean advanceServerSeed) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("values must be a non-empty list");
        }

        int totalWeight = values.stream()
                .map(r -> r.getWeight() == null ? 0 : r.getWeight())
                .reduce(0, Integer::sum);
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("Sum of weights must be positive");
        }

        RollContext ctx = new RollContext(serverSeed, clientSeed, nonce);
        int valueValue = Prf.drawInt(ctx, "value");
        String valueMsg = buildMessage(clientSeed, nonce, "value");
        String valueHmacHex = Prf.drawHex(ctx, "value");
        Integer valueRoll = Math.floorMod(valueValue, totalWeight);

        Value chosenValue = getChosenValue(values, valueValue, totalWeight);

        int itemValue = Prf.drawInt(ctx, "item");
        String itemMsg = buildMessage(clientSeed, nonce, "item");
        String itemHmacHex = Prf.drawHex(ctx, "item");

        Integer itemsInValue = chosenValue.getCount();
        Integer itemIndex = null;
        if (itemsInValue != null && itemsInValue > 0) {
            itemIndex = Math.floorMod(itemValue, itemsInValue);
        }

        String nextServerSeed = advanceServerSeed ? seedChainAdvance(serverSeed) : serverSeed;

        Rollout rollout = new Rollout(chosenValue.getId(), itemIndex, Debug.builder()
                .serverSeedUsed(serverSeed)
                .clientSeed(clientSeed)
                .nonce(nonce)
                .valueMsg(valueMsg)
                .valueHmacHex(valueHmacHex)
                .hmacValueNumericPrefix(Integer.toUnsignedLong(valueValue))
                .selectedValueWeight(chosenValue.getWeight())
                .itemMsg(itemMsg)
                .itemHmacHex(itemHmacHex)
                .hmacItemNumericPrefix(Integer.toUnsignedLong(itemValue))
                .itemIndex(itemIndex)
                .itemsInValue(itemsInValue)
                .totalWeight(totalWeight)
                .rollValue(valueRoll)
        .build());

        return new FairRoll(rollout, nextServerSeed);
    }

    private static Value getChosenValue(List<Value> values, int valueValue, int totalWeight) {
        int valueRoll = Math.floorMod(valueValue, totalWeight);

        int cumulative = 0;
        Value chosenValue = null;
        for (Value value : values) {
            int w = value.getWeight() == null ? 0 : value.getWeight();
            cumulative += w;
            if (valueRoll < cumulative) {
                chosenValue = value;
                break;
            }
        }
        if (chosenValue == null) {
            throw new IllegalStateException("Failed to select a value from weights");
        }
        return chosenValue;
    }

    /**
     * Validates the HMAC-SHA256 of a given message using a given secret key.
     * This method is safe against timing attacks, as it uses the constant-time
     * comparison provided by MessageDigest.isEqual on the bytes of the actual and expected
     * HMACs.
     *
     * @param secretKey the secret key to use for the HMAC computation
     * @param message the message to compute the HMAC of
     * @param expectedHmacHex the expected HMAC of the message as a hexadecimal string
     * @return whether the actual HMAC matches the expected HMAC
     */
    public static boolean validateHmac(String secretKey, String message, String expectedHmacHex) {
        String actual = hmacHex(secretKey, message);
        // Constant-time comparison using MessageDigest.isEqual on bytes
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                expectedHmacHex.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Build a message string used for HMAC computation.
     * The message consists of three parts separated by colons:
     * <ul>
     * <li>Client seed</li>
     * <li>Nonce</li>
     * <li>Category</li>
     * </ul>
     *
     * @param clientSeed the client seed to use
     * @param nonce the nonce to use
     * @param category the category to use
     * @return the message string
     */
    public static String buildMessage(String clientSeed, long nonce, String category) {
        return clientSeed + ":" + nonce + ":" + category;
    }


    private static int firstFourBytesAsInt(byte[] bytes) {
        // Use big-endian of first 4 bytes; treat as unsigned by using toUnsignedLong when needed
        ByteBuffer bb = ByteBuffer.wrap(bytes, 0, 4);
        return bb.getInt();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Immutable context for a provably fair roll operation.
     * <p>
     * Contains the three inputs required to generate deterministic pseudorandom values:
     * the server seed (secret), the client seed (public), and a nonce (counter).
     *
     * @param serverSeed the server's secret seed used as the HMAC key
     * @param clientSeed the client's public seed included in the HMAC message
     * @param nonce a counter value to ensure unique outputs for each roll
     */
    public record RollContext(String serverSeed, String clientSeed, long nonce) { }

    /**
     * Result of a provably fair roll operation.
     * <p>
     * Contains the roll result with debug information and the next server seed
     * (if seed advancement was requested).
     *
     * @param result a map containing the selected value ID, item index, and debug information
     * @param nextServerSeed the server seed to use for the next roll (may be the same or advanced)
     */
    public record FairRoll(Rollout result, String nextServerSeed) { }
}
