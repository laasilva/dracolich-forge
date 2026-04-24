package dm.dracolich.forge.security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AnonCookieSignerTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-abcdef";

    private final AnonCookieSigner signer = new AnonCookieSigner(SECRET);

    @Nested
    class Construction {

        @Test
        void rejectsNullSecret() {
            assertThrows(IllegalArgumentException.class, () -> new AnonCookieSigner(null));
        }

        @Test
        void rejectsShortSecret() {
            assertThrows(IllegalArgumentException.class, () -> new AnonCookieSigner("too-short"));
        }
    }

    @Nested
    class SignAndParse {

        @Test
        void roundTripsValidCookie() {
            String signed = signer.sign("anon-abc", Instant.now().plusSeconds(3600));
            Optional<String> parsed = signer.parse(signed);
            assertTrue(parsed.isPresent());
            assertEquals("anon-abc", parsed.get());
        }

        @Test
        void rejectsTamperedAnonId() {
            String signed = signer.sign("anon-abc", Instant.now().plusSeconds(3600));
            String tampered = "anon-xyz" + signed.substring("anon-abc".length());
            assertTrue(signer.parse(tampered).isEmpty());
        }

        @Test
        void rejectsTamperedSignature() {
            String signed = signer.sign("anon-abc", Instant.now().plusSeconds(3600));
            String tampered = signed.substring(0, signed.length() - 3) + "XXX";
            assertTrue(signer.parse(tampered).isEmpty());
        }

        @Test
        void rejectsExpiredCookie() {
            String signed = signer.sign("anon-abc", Instant.now().minusSeconds(1));
            assertTrue(signer.parse(signed).isEmpty());
        }

        @Test
        void rejectsMalformedCookie() {
            assertTrue(signer.parse("garbage").isEmpty());
            assertTrue(signer.parse("one.two").isEmpty());
            assertTrue(signer.parse(null).isEmpty());
            assertTrue(signer.parse("").isEmpty());
        }

        @Test
        void rejectsSignatureFromDifferentSecret() {
            AnonCookieSigner other = new AnonCookieSigner("other-secret-other-secret-other-secret-xyz");
            String signed = other.sign("anon-abc", Instant.now().plusSeconds(3600));
            assertTrue(signer.parse(signed).isEmpty());
        }
    }
}
