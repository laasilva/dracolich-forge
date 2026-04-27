package dm.dracolich.forge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

public class EcPublicKeyJwtValidator implements JwtTokenValidator {

    private static final String BEGIN_MARKER = "-----BEGIN PUBLIC KEY-----";
    private static final String END_MARKER = "-----END PUBLIC KEY-----";

    private final ECPublicKey publicKey;

    public EcPublicKeyJwtValidator(ECPublicKey publicKey) {
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
    }

    public static EcPublicKeyJwtValidator fromResource(Resource pemResource) {
        Objects.requireNonNull(pemResource, "pemResource");
        try (InputStream is = pemResource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return new EcPublicKeyJwtValidator(parsePem(new String(bytes, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read public key resource: " + pemResource, e);
        }
    }

    public static EcPublicKeyJwtValidator fromPem(String pem) {
        Objects.requireNonNull(pem, "pem");
        return new EcPublicKeyJwtValidator(parsePem(pem));
    }

    @Override
    public Mono<Claims> validate(String token) {
        return Mono.fromCallable(() -> Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload());
    }

    private static ECPublicKey parsePem(String pem) {
        String stripped = pem.replace(BEGIN_MARKER, "")
                .replace(END_MARKER, "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(stripped);
        try {
            return (ECPublicKey) KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to parse EC public key", e);
        }
    }
}
