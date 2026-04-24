package dm.dracolich.forge.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AnonCookieFilter implements WebFilter, Ordered {

    public static final int ORDER = JwtAuthenticationWebFilter.ORDER + 10;
    public static final String COOKIE_NAME = "dracolich_anon_id";

    private static final Logger log = LoggerFactory.getLogger(AnonCookieFilter.class);

    private final AnonCookieSigner signer;
    private final Duration cookieLifetime;
    private final boolean secureCookie;

    public AnonCookieFilter(AnonCookieSigner signer, Duration cookieLifetime, boolean secureCookie) {
        this.signer = signer;
        this.cookieLifetime = cookieLifetime;
        this.secureCookie = secureCookie;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .filter(ctx -> ctx.getAuthentication() != null
                        && ctx.getAuthentication().getPrincipal() instanceof Principal)
                .hasElement()
                .flatMap(alreadyAuthed -> Boolean.TRUE.equals(alreadyAuthed)
                        ? chain.filter(exchange)
                        : applyAnonPrincipal(exchange, chain));
    }

    private Mono<Void> applyAnonPrincipal(ServerWebExchange exchange, WebFilterChain chain) {
        Optional<String> existingAnonId = readAnonId(exchange);
        String anonId = existingAnonId.orElseGet(() -> UUID.randomUUID().toString());
        if (existingAnonId.isEmpty()) {
            writeCookie(exchange, anonId);
            log.debug("Minted new anon cookie for {}", exchange.getRequest().getPath());
        }
        Principal principal = Principal.anon(anonId);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
    }

    private Optional<String> readAnonId(ServerWebExchange exchange) {
        return readAnonIdCookie(exchange, signer);
    }

    public static Optional<String> readAnonIdCookie(ServerWebExchange exchange, AnonCookieSigner signer) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(COOKIE_NAME);
        if (cookie == null) {
            return Optional.empty();
        }
        return signer.parse(cookie.getValue());
    }

    private void writeCookie(ServerWebExchange exchange, String anonId) {
        Instant expiresAt = Instant.now().plus(cookieLifetime);
        String value = signer.sign(anonId, expiresAt);
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(cookieLifetime)
                .build();
        exchange.getResponse().addCookie(cookie);
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
