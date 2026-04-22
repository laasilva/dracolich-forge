package dm.dracolich.forge.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

public class JwtAuthenticationWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationWebFilter.class);

    private final JwtTokenValidator tokenValidator;

    public JwtAuthenticationWebFilter(JwtTokenValidator tokenValidator) {
        this.tokenValidator = tokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);
        return tokenValidator.validate(token)
                .map(claims -> {
                    String accessLevel = claims.get("accessLevel", String.class);
                    var authorities = accessLevel != null
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + accessLevel))
                            : List.<SimpleGrantedAuthority>of();
                    return new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null, authorities);
                })
                .flatMap(auth -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .onErrorResume(e -> {
                    log.warn("JWT authentication failed for {}: {}", exchange.getRequest().getPath(), e.getMessage());
                    return chain.filter(exchange);
                });
    }
}
