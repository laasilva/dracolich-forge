package dm.dracolich.forge.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AnonCookieFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-abcdef";
    private static final Duration LIFETIME = Duration.ofHours(24);

    private AnonCookieSigner signer;
    private AnonCookieFilter filter;

    @BeforeEach
    void setUp() {
        signer = new AnonCookieSigner(SECRET);
        filter = new AnonCookieFilter(signer, LIFETIME, false);
    }

    @Nested
    class WhenNoJwt {

        @Test
        void mintsCookieAndSetsAnonPrincipalWhenCookieAbsent() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/decks").build());

            AtomicReference<SecurityContext> capturedContext = new AtomicReference<>();
            WebFilterChain chain = ex -> ReactiveSecurityContextHolder.getContext()
                    .doOnNext(capturedContext::set)
                    .then();

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            assertNotNull(capturedContext.get());
            Principal principal = (Principal) capturedContext.get().getAuthentication().getPrincipal();
            assertEquals(PrincipalType.ANON, principal.type());
            assertNotNull(principal.id());

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst(AnonCookieFilter.COOKIE_NAME);
            assertNotNull(cookie);
            assertTrue(cookie.isHttpOnly());
            assertEquals("Lax", cookie.getSameSite());
            assertEquals(principal.id(), signer.parse(cookie.getValue()).orElseThrow());
        }

        @Test
        void reusesExistingCookieWithoutMintingAgain() {
            String anonId = "anon-existing";
            String signedValue = signer.sign(anonId, Instant.now().plus(LIFETIME));
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/decks")
                            .cookie(new org.springframework.http.HttpCookie(AnonCookieFilter.COOKIE_NAME, signedValue))
                            .build());

            AtomicReference<SecurityContext> capturedContext = new AtomicReference<>();
            WebFilterChain chain = ex -> ReactiveSecurityContextHolder.getContext()
                    .doOnNext(capturedContext::set)
                    .then();

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            Principal principal = (Principal) capturedContext.get().getAuthentication().getPrincipal();
            assertEquals(PrincipalType.ANON, principal.type());
            assertEquals(anonId, principal.id());
            assertNull(exchange.getResponse().getCookies().getFirst(AnonCookieFilter.COOKIE_NAME),
                    "no new cookie should be set when existing cookie is valid");
        }

        @Test
        void mintsNewCookieWhenExistingCookieIsTampered() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/decks")
                            .cookie(new org.springframework.http.HttpCookie(AnonCookieFilter.COOKIE_NAME, "tampered.garbage.value"))
                            .build());

            WebFilterChain chain = ex -> Mono.empty();

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            ResponseCookie cookie = exchange.getResponse().getCookies().getFirst(AnonCookieFilter.COOKIE_NAME);
            assertNotNull(cookie, "a new cookie should be minted when the existing one is invalid");
        }

        @Test
        void mintsNewCookieWhenExistingCookieIsExpired() {
            String anonId = "anon-old";
            String signedValue = signer.sign(anonId, Instant.now().minusSeconds(1));
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/decks")
                            .cookie(new org.springframework.http.HttpCookie(AnonCookieFilter.COOKIE_NAME, signedValue))
                            .build());

            AtomicReference<SecurityContext> capturedContext = new AtomicReference<>();
            WebFilterChain chain = ex -> ReactiveSecurityContextHolder.getContext()
                    .doOnNext(capturedContext::set)
                    .then();

            StepVerifier.create(filter.filter(exchange, chain))
                    .verifyComplete();

            Principal principal = (Principal) capturedContext.get().getAuthentication().getPrincipal();
            assertNotEquals(anonId, principal.id(), "expired cookie's anon_id should not be reused");
            assertNotNull(exchange.getResponse().getCookies().getFirst(AnonCookieFilter.COOKIE_NAME));
        }
    }

    @Nested
    class WhenJwtAlreadyAuthenticated {

        @Test
        void doesNotMintCookieOrOverrideUserPrincipal() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/decks").build());

            Principal userPrincipal = Principal.user("user-42");
            var auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, List.of());

            AtomicReference<SecurityContext> capturedContext = new AtomicReference<>();
            WebFilterChain chain = ex -> ReactiveSecurityContextHolder.getContext()
                    .doOnNext(capturedContext::set)
                    .then();

            Mono<Void> filtered = filter.filter(exchange, chain)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

            StepVerifier.create(filtered)
                    .verifyComplete();

            Principal principal = (Principal) capturedContext.get().getAuthentication().getPrincipal();
            assertEquals(PrincipalType.USER, principal.type());
            assertEquals("user-42", principal.id());
            assertNull(exchange.getResponse().getCookies().getFirst(AnonCookieFilter.COOKIE_NAME));
        }
    }

    @Test
    void secureFlagReflectsConstructorArgument() {
        AnonCookieFilter secureFilter = new AnonCookieFilter(signer, LIFETIME, true);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/decks").build());

        WebFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(secureFilter.filter(exchange, chain))
                .verifyComplete();

        ResponseCookie cookie = exchange.getResponse().getCookies().getFirst(AnonCookieFilter.COOKIE_NAME);
        assertTrue(cookie.isSecure());
    }

    @Test
    void orderIsAfterJwtFilter() {
        assertTrue(filter.getOrder() > JwtAuthenticationWebFilter.ORDER);
    }

    @Nested
    class ReadAnonIdCookieHelper {

        @Test
        void returnsAnonIdFromValidCookie() {
            String anonId = "anon-claim-target";
            String signedValue = signer.sign(anonId, Instant.now().plus(LIFETIME));
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/decks/1/claim")
                            .cookie(new org.springframework.http.HttpCookie(AnonCookieFilter.COOKIE_NAME, signedValue))
                            .build());

            Optional<String> result = AnonCookieFilter.readAnonIdCookie(exchange, signer);

            assertTrue(result.isPresent());
            assertEquals(anonId, result.get());
        }

        @Test
        void emptyWhenNoCookiePresent() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/decks/1/claim").build());
            assertTrue(AnonCookieFilter.readAnonIdCookie(exchange, signer).isEmpty());
        }

        @Test
        void emptyWhenCookieTampered() {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.get("/decks/1/claim")
                            .cookie(new org.springframework.http.HttpCookie(AnonCookieFilter.COOKIE_NAME, "bad.cookie.value"))
                            .build());
            assertTrue(AnonCookieFilter.readAnonIdCookie(exchange, signer).isEmpty());
        }
    }
}
