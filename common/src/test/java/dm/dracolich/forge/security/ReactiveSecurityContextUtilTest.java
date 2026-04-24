package dm.dracolich.forge.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class ReactiveSecurityContextUtilTest {

    @Test
    void returnsPrincipalWhenPresent() {
        Principal principal = Principal.user("user-1");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        Mono<Principal> result = ReactiveSecurityContextUtil.getPrincipal()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        StepVerifier.create(result)
                .expectNext(principal)
                .verifyComplete();
    }

    @Test
    void returnsAnonPrincipalWhenPresent() {
        Principal principal = Principal.anon("anon-1");
        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

        Mono<Principal> result = ReactiveSecurityContextUtil.getPrincipal()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        StepVerifier.create(result)
                .expectNext(principal)
                .verifyComplete();
    }

    @Test
    void emptyWhenNoSecurityContext() {
        StepVerifier.create(ReactiveSecurityContextUtil.getPrincipal())
                .verifyComplete();
    }

    @Test
    void emptyWhenAuthenticationHasForeignPrincipalType() {
        var auth = new UsernamePasswordAuthenticationToken("raw-string", null, List.of());

        Mono<Principal> result = ReactiveSecurityContextUtil.getPrincipal()
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        StepVerifier.create(result)
                .verifyComplete();
    }
}
