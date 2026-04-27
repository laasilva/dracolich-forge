package dm.dracolich.forge.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class EcPublicKeyJwtValidatorTest {

    private static ECPrivateKey privateKey;
    private static ECPublicKey publicKey;
    private static String publicKeyPem;

    @BeforeAll
    static void generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = (ECPrivateKey) keyPair.getPrivate();
        publicKey = (ECPublicKey) keyPair.getPublic();

        String encoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
    }

    private static String signToken(String subject, long expiresInSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("accessLevel", "COMMON_USER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(privateKey, Jwts.SIG.ES384)
                .compact();
    }

    @Nested
    class Construction {

        @Test
        void rejectsNullPublicKey() {
            assertThrows(NullPointerException.class, () -> new EcPublicKeyJwtValidator(null));
        }

        @Test
        void rejectsNullResource() {
            assertThrows(NullPointerException.class, () -> EcPublicKeyJwtValidator.fromResource(null));
        }

        @Test
        void rejectsNullPem() {
            assertThrows(NullPointerException.class, () -> EcPublicKeyJwtValidator.fromPem(null));
        }

        @Test
        void rejectsMalformedPem() {
            assertThrows(Exception.class, () -> EcPublicKeyJwtValidator.fromPem("not a real key"));
        }
    }

    @Nested
    class Validation {

        @Test
        void parsesValidTokenFromPublicKey() {
            String token = signToken("user-123", 900);
            EcPublicKeyJwtValidator validator = new EcPublicKeyJwtValidator(publicKey);

            StepVerifier.create(validator.validate(token))
                    .assertNext(claims -> {
                        assertEquals("user-123", claims.getSubject());
                        assertEquals("COMMON_USER", claims.get("accessLevel"));
                    })
                    .verifyComplete();
        }

        @Test
        void parsesValidTokenFromPemResource() {
            String token = signToken("user-456", 900);
            EcPublicKeyJwtValidator validator = EcPublicKeyJwtValidator.fromResource(
                    new ByteArrayResource(publicKeyPem.getBytes()));

            StepVerifier.create(validator.validate(token))
                    .assertNext(claims -> assertEquals("user-456", claims.getSubject()))
                    .verifyComplete();
        }

        @Test
        void parsesValidTokenFromPemString() {
            String token = signToken("user-789", 900);
            EcPublicKeyJwtValidator validator = EcPublicKeyJwtValidator.fromPem(publicKeyPem);

            StepVerifier.create(validator.validate(token))
                    .assertNext(claims -> assertEquals("user-789", claims.getSubject()))
                    .verifyComplete();
        }

        @Test
        void rejectsExpiredToken() {
            String token = signToken("user-expired", -3600);
            EcPublicKeyJwtValidator validator = new EcPublicKeyJwtValidator(publicKey);

            StepVerifier.create(validator.validate(token))
                    .expectError(JwtException.class)
                    .verify();
        }

        @Test
        void rejectsTamperedToken() {
            String token = signToken("user-123", 900);
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            EcPublicKeyJwtValidator validator = new EcPublicKeyJwtValidator(publicKey);

            StepVerifier.create(validator.validate(tampered))
                    .expectError(JwtException.class)
                    .verify();
        }

        @Test
        void rejectsTokenSignedByDifferentKey() throws Exception {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(new ECGenParameterSpec("secp384r1"));
            ECPrivateKey otherPrivateKey = (ECPrivateKey) gen.generateKeyPair().getPrivate();

            String token = Jwts.builder()
                    .subject("user-attacker")
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plusSeconds(900)))
                    .signWith(otherPrivateKey, Jwts.SIG.ES384)
                    .compact();

            EcPublicKeyJwtValidator validator = new EcPublicKeyJwtValidator(publicKey);

            StepVerifier.create(validator.validate(token))
                    .expectError(JwtException.class)
                    .verify();
        }
    }
}
