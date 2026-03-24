package dm.dracolich.forge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationWebFilterTest {

    private JwtAuthenticationWebFilter filter;
    private ECPrivateKey privateKey;
    private ECPublicKey publicKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair keyPair = generator.generateKeyPair();
        privateKey = (ECPrivateKey) keyPair.getPrivate();
        publicKey = (ECPublicKey) keyPair.getPublic();

        JwtTokenValidator validator = token -> Mono.fromCallable(() ->
                Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
        );

        filter = new JwtAuthenticationWebFilter(validator);
    }

    private String generateToken(String subject, String accessLevel) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(privateKey, Jwts.SIG.ES384);
        if (accessLevel != null) {
            builder.claim("accessLevel", accessLevel);
        }
        return builder.compact();
    }

    private ServerWebExchange mockExchange(String authHeaderValue) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeaderValue != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeaderValue);
        }
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getHeaders()).thenReturn(headers);
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getRequest()).thenReturn(request);
        return exchange;
    }

    @Test
    void validToken_setsSecurityContext() {
        String token = generateToken("user-123", "ADMIN");
        ServerWebExchange exchange = mockExchange("Bearer " + token);

        AtomicReference<SecurityContext> capturedContext = new AtomicReference<>();

        WebFilterChain chain = ex -> ReactiveSecurityContextHolder.getContext()
                .doOnNext(capturedContext::set)
                .then();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertNotNull(capturedContext.get());
        assertEquals("user-123", capturedContext.get().getAuthentication().getPrincipal());
        assertTrue(capturedContext.get().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void noAuthHeader_passesThrough() {
        ServerWebExchange exchange = mockExchange(null);

        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        WebFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainCalled.get());
    }

    @Test
    void nonBearerHeader_passesThrough() {
        ServerWebExchange exchange = mockExchange("Basic dXNlcjpwYXNz");

        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        WebFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainCalled.get());
    }

    @Test
    void invalidToken_passesThrough() {
        ServerWebExchange exchange = mockExchange("Bearer invalid.jwt.token");

        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        WebFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainCalled.get());
    }

    @Test
    void expiredToken_passesThrough() {
        Instant past = Instant.now().minusSeconds(3600);
        String expiredToken = Jwts.builder()
                .subject("user-123")
                .claim("accessLevel", "COMMON_USER")
                .issuedAt(Date.from(past.minusSeconds(900)))
                .expiration(Date.from(past))
                .signWith(privateKey, Jwts.SIG.ES384)
                .compact();

        ServerWebExchange exchange = mockExchange("Bearer " + expiredToken);

        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);
        WebFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertTrue(chainCalled.get());
    }

    @Test
    void tokenWithoutAccessLevel_setsEmptyAuthorities() {
        String token = generateToken("user-456", null);
        ServerWebExchange exchange = mockExchange("Bearer " + token);

        AtomicReference<SecurityContext> capturedContext = new AtomicReference<>();

        WebFilterChain chain = ex -> ReactiveSecurityContextHolder.getContext()
                .doOnNext(capturedContext::set)
                .then();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertNotNull(capturedContext.get());
        assertEquals("user-456", capturedContext.get().getAuthentication().getPrincipal());
        assertTrue(capturedContext.get().getAuthentication().getAuthorities().isEmpty());
    }
}
